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
import androidx.core.app.NotificationCompat

/**
 * MAYA Wake Word Service (Phase 4)
 * Background mein hamesha sunta hai — "Maya" ya "Boss" sunte hi app ko signal deta hai.
 * Notification mein chalta hai (Android rule) — battery-friendly rakha gaya hai.
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
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        startAsForeground()
        startLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        running = false
        try { sr?.destroy(); sr = null } catch (e: Exception) {}
        super.onDestroy()
    }

    private fun startAsForeground() {
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
            .setContentTitle("MAYA sun rahi hai \uD83D\uDC42")
            .setContentText("Bolo: \u201CMaya\u201D ya \u201CBoss\u201D")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pi)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
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

    private fun startLoop() {
        handler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                evalToApp("window.__wakeErr && window.__wakeErr(5)")
                stopSelf()
                return@post
            }
            stopRecognizer()
            sr = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        when (error) {
                            6, 7 -> restart(250)          // timeout / no-match — turant dobara
                            1, 2 -> restart(2000)          // network — thora intezar
                            4 -> restart(1200)             // busy
                            8 -> {                        // permission — band
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
                        if (text.isNotBlank()) {
                            evalToApp("window.__wakeHeard && window.__wakeHeard('" + jsEsc(text) + "')")
                        }
                        restart(200)
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                actuallyStart()
            }
        }
    }

    private fun restart(delay: Long) {
        handler.postDelayed({ actuallyStart() }, delay)
    }

    private fun actuallyStart() {
        if (!running) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            sr?.startListening(intent)
        } catch (e: Exception) {}
    }

    private fun stopRecognizer() {
        try { sr?.destroy(); sr = null } catch (e: Exception) {}
    }
}
