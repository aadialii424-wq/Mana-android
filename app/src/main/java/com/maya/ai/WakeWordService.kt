package com.maya.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.content.pm.ServiceInfo
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat

/**
 * MAYA Wake Word Service 2.0 (Phase 10: ALWAYS-ON)
 * - Background mein hamesha sunta hai — "Maya" ya "Boss"
 * - App khula ho → WebView ke raaste poora flow (chime + listen + kaam)
 * - App band ho → apna TTS bolta hai + khud app khol deta hai
 * - Watchdog: har 45s check; har 12 min recognizer refresh
 */
class WakeWordService : Service() {

    companion object {
        const val CHANNEL_ID = "maya_wake"
        const val NOTIF_ID = 2001

        fun start(ctx: Context) {
            try {
                val i = Intent(ctx, WakeWordService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
                else ctx.startService(i)
            } catch (e: Exception) {}
        }

        fun stop(ctx: Context) {
            try { ctx.stopService(Intent(ctx, WakeWordService::class.java)) } catch (e: Exception) {}
        }
    }

    private var sr: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var running = false
    private var watchdogRuns = 0
    private var lastWakeAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        startAsForeground()
        try {
            tts = TextToSpeech(this) { st -> ttsReady = st == TextToSpeech.SUCCESS }
        } catch (e: Exception) {}
        startLoop()
        handler.postDelayed(::watchdog, 45000)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        running = false
        handler.removeCallbacksAndMessages(null)
        try { sr?.destroy(); sr = null } catch (e: Exception) {}
        try { tts?.stop(); tts?.shutdown() } catch (e: Exception) {}
        super.onDestroy()
    }

    private fun startAsForeground() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "MAYA Wake Word", NotificationManager.IMPORTANCE_LOW)
                )
            }
            val pi = PendingIntent.getActivity(
                this, 1, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notif: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("MAYA hamesha sun rahi hai \uD83D\uDC42")
                .setContentText("Bolo: \u201CMaya\u201D ya \u201CBoss\u201D — kahin se bhi")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pi)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTIF_ID, notif)
            }
        } catch (e: Exception) {}
    }

    private fun speakLocal(text: String) {
        try {
            if (ttsReady) {
                tts?.language = java.util.Locale.forLanguageTag("ur-PK")
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "wake_" + System.currentTimeMillis())
            }
        } catch (e: Exception) {}
    }

    private fun evalToApp(js: String) {
        try {
            val act = MainActivity.instance ?: return
            act.evalAsyncPublic(js)
        } catch (e: Exception) {}
    }

    private fun jsEsc(s: String): String = s
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\"", "\\\"")
        .replace("\n", " ")
        .replace("\r", " ")

    private fun launchApp() {
        try {
            val i = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(i)
        } catch (e: Exception) {}
    }

    private fun startLoop() {
        handler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                evalToApp("window.__wakeErr && window.__wakeErr(5)")
                stopSelf()
                return@post
            }
            resetRecognizer()
            actuallyStart()
        }
    }

    private fun resetRecognizer() {
        try { sr?.destroy() } catch (e: Exception) {}
        sr = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    when (error) {
                        6, 7 -> restart(250)
                        1, 2 -> restart(2000)
                        4 -> restart(1200)
                        8 -> {
                            evalToApp("window.__wakeErr && window.__wakeErr(8)")
                            stopSelf()
                        }
                        else -> restart(900)
                    }
                }
                override fun onResults(results: Bundle?) {
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull() ?: ""
                    if (text.isNotBlank()) handle(text)
                    restart(200)
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    /** ASLI DIMAAG: app khula ho to JS ko; band ho to khud kaam karo */
    private fun handle(text: String) {
        val t = text.lowercase()
        val isWake = t.contains("maya") || t.contains("maywa") || t.contains("mya") ||
            t.contains("maaya") || t.contains("boss") || t.contains("\\u0645\\u0627\\u06CC\\u0627")
        val appAlive = MainActivity.instance != null
        if (appAlive) {
            evalToApp("window.__wakeHeard && window.__wakeHeard('" + jsEsc(text) + "')")
            return
        }
        /* SAFE MODE (v2.12.1): app band ho to KUCH NA KARO —
           v2.10.0 ka khud-app-kholna engine hi black screen ka mujrim nikla.
           App khuli ho to wake word poora kaam karta hai. */
    }

    private fun restart(delay: Long) {
        handler.postDelayed({ actuallyStart() }, delay)
    }

    private fun actuallyStart() {
        if (!running) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            sr?.startListening(intent)
        } catch (e: Exception) {}
    }

    /** Har 45s zinda hai? har 12 min fresh recognizer */
    private fun watchdog() {
        if (!running) return
        watchdogRuns++
        if (watchdogRuns >= 16) { // ~12 min
            watchdogRuns = 0
            resetRecognizer()
            actuallyStart()
        }
        handler.postDelayed(::watchdog, 45000)
    }
}
