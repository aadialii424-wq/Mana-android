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
import org.json.JSONArray
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

        @Volatile var instance: WakeWordService? = null
        @Volatile var haal: String = "KHALI"
        @Volatile var lastBolAt: Long = 0L
        @Volatile var pausedByApp: Boolean = false
        @Volatile var pausedAt: Long = 0L
        const val ECHO_TAIL_MS = 550L

        fun start(ctx: Context) {
            try {
                val i = Intent(ctx, WakeWordService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
                else ctx.startService(i)
            } catch (e: Exception) {}
        }

        fun stop(ctx: Context) {
            haal = "KHALI"
            pausedByApp = false
            try { ctx.stopService(Intent(ctx, WakeWordService::class.java)) } catch (e: Exception) {}
        }

        fun setHaal(h: String) {
            if (h == "BOL_RAHI") lastBolAt = System.currentTimeMillis()
            haal = h
            try { instance?.onHaal(h) } catch (e: Exception) {}
        }

        fun haalBlock(): String? {
            val s = instance ?: return null
            if (!s.sukoonOn()) return null
            if (haal == "BOL_RAHI") return "Maya bol rahi hai"
            if (haal == "APP_SUN") return "app ka mic chal raha hai"
            if (pausedByApp) return "sulah: app ka mic"
            if (System.currentTimeMillis() - lastBolAt < ECHO_TAIL_MS) return "echo tail"
            return null
        }
    }

    private var sr: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var running = false
    private var watchdogRuns = 0
    private var lastWakeAt = 0L
    private var errStreak = 0
    private var lastErr = 0
    private var starts = 0

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
        stopGate()
        try { MicKit.release() } catch (e: Exception) {}
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

    /* ═══════════════════════════════════════════════════════════════════
       🎧 KHAMOSHI KA PEHRA (VAD) — P8c
       -------------------------------------------------------------------
       Pehle recognizer SANNATE mein bhi har 1-3 second chalta rehta tha.
       Android 11+ background mic ko throttle karta hai -> "mic on/off".

       Ab: sasta AudioRecord chalta hai (mic zoom + shor-kush ke sath).
       Sannata -> recognizer BILKUL band. Awaaz aayi -> mic chhor kar
       recognizer chalao. Jawab aaya -> wapas pehre par.

       Mic ek waqt mein ek hi cheez ke paas ho sakta hai — is liye pehra
       aur recognizer kabhi ek sath nahi chalte.
       ═══════════════════════════════════════════════════════════════════ */
    @Volatile private var gateOn = false
    private var gateThread: Thread? = null
    private var floorDb = 0.0

    private fun vadEnabled(): Boolean = try {
        getSharedPreferences("maya", Context.MODE_PRIVATE).getBoolean("mic_near", true)
    } catch (e: Exception) { true }

    private fun micZoom(): Float = try {
        getSharedPreferences("maya", Context.MODE_PRIVATE).getString("mic_zoom", "0.8")!!.toFloat()
    } catch (e: Exception) { 0.8f }

    private fun startGate() {
        if (gateOn) return
        gateOn = true
        gateThread = Thread {
            val rec = MicKit.open(micZoom())
            if (rec == null) {
                gateOn = false
                report("gate", "mic nahi khula \u2014 seedha recognizer")
                handler.post { actuallyStart() }
                return@Thread
            }
            report("gate", "pehra shuru  zoom:" + (if (MicKit.fxZoom) "\u2713" else "\u2717") +
                   " ns:" + (if (MicKit.fxNs) "\u2713" else "\u2717"))
            val buf = ShortArray(1600)
            var quiet = 0
            var loud = 0
            floorDb = 0.0
            try {
                rec.startRecording()
                while (gateOn && running) {
                    val n = rec.read(buf, 0, buf.size)
                    if (n <= 0) continue
                    val d = MicKit.db(buf, n)
                    if (floorDb <= 0.0) floorDb = d
                    if (d < floorDb) floorDb = floorDb * 0.9 + d * 0.1     /* farsh dheere dheere seekho */
                    val over = d - floorDb
                    if (over > 14.0) { loud++; quiet = 0 } else { quiet++; if (quiet > 3) loud = 0 }
                    if (loud >= 3) {                                       /* ~300ms qareebi awaaz */
                        report("voice", "awaaz " + Math.round(d) + "dB  farsh " + Math.round(floorDb) + "dB")
                        break
                    }
                }
                rec.stop()
            } catch (e: Exception) {
                report("gate", "pehra nakaam: " + (e.message ?: "?"))
            }
            try { rec.release() } catch (e: Exception) {}
            MicKit.release()
            gateOn = false
            if (running) handler.post { actuallyStart() }                  /* ab recognizer ki baari */
        }
        gateThread?.start()
    }

    private fun stopGate() { gateOn = false }

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
        /* 🎯 P8b — wahi seerhi jo MainActivity mein hai: on-device -> Google -> aam.
           Android 12+ par default AiAi ho sakta hai jo kaam hi nahi karta. */
        sr = (MainActivity.instance?.makeRecognizer()
              ?: SpeechRecognizer.createSpeechRecognizer(this)).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    /* v5.7.0 — pehle NO_MATCH par sirf 250ms baad dobara shuru
                       hota tha. Android 11+ background mic ko THROTTLE karta hai
                       aur itni tez restart par Google ka recognizer chup ho jata
                       hai — yehi "mic on hota hai band hota hai" ki wajah thi.
                       Ab har lagatar nakami par intezar barhta jata hai. */
                    errStreak++
                    lastErr = error
                    report("err", error.toString() + "|" + errStreak)
                    val back = when (error) {
                        6, 7 -> 700L + (errStreak.coerceAtMost(8) * 350L)   /* 0.7s -> 3.5s */
                        1, 2 -> 3000L
                        4 -> 1500L
                        8 -> {
                            /* L5 ERR-8 MERCY — RECOGNIZER_BUSY: mic kisi aur ke paas.
                               Na stopSelf, na switch. 2s sukoon, phir koshish. */
                            errStreak = 0
                            report("err8", "mic masroof — 2s baad phir")
                            restart(2000)
                            return
                        }
                        else -> 1200L
                    }
                    restart(back)
                }
                override fun onResults(results: Bundle?) {
                    val all = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?: arrayListOf()
                    if (all.isNotEmpty()) { handleAll(all); errStreak = 0 }
                    restart(400)
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    /**
     * v5.7.0 — Kotlin ab FAISLA NAHI karta, sirf REPORT karta hai.
     *
     * Pehle yahan `isWake` bana kar CHHOR diya jata tha (dead variable), aur
     * Urdu ka check "\\u0645..." tha — yani literal matn, kabhi match hi nahi
     * hota tha. Ab saare andaze JS ko jate hain aur wahan faisla hota hai.
     *
     * Faida: aage wake-word ki tuning ke liye NAYI APK nahi banani paregi.
     */
    private fun report(kind: String, payload: String) {
        evalToApp("window.__wakeLog && window.__wakeLog('" + jsEsc(kind) + "','" + jsEsc(payload) + "')")
    }

    private fun handleAll(list: List<String>) {
        val arr = JSONArray()
        for (i in list.indices) { if (i >= 6) break; arr.put(list[i]) }
        val payload = arr.toString()
        if (MainActivity.instance != null) {
            evalToApp("window.__wakeHeard && window.__wakeHeard('" + jsEsc(payload) + "')")
        } else {
            /* SAFE MODE: app band ho to KUCH NA KARO — v2.10.0 ka khud-app-kholna
               engine hi black screen ka mujrim nikla tha. */
            lastHeardOffline = payload
        }
    }
    private var lastHeardOffline = ""

    private fun restart(delay: Long) {
        /* P8c — seedha recognizer nahi; pehle KHAMOSHI KA PEHRA. Sannate mein
           recognizer bilkul nahi chalega -> "mic on/off" khatam. */
        handler.postDelayed({
            if (!running) return@postDelayed
            if (vadEnabled()) startGate() else actuallyStart()
        }, delay)
    }

    private fun actuallyStart() {
        if (!running) return
        try {
            /* v5.7.0 — do badlaav:
               1. MAX_RESULTS 1 -> 6. SUNO ka sabaq: sahih jawab aksar doosre ya
                  teesre andaze mein hota hai. Wake word par ye aur zyada ahem hai.
               2. Zubaan ab settings se aati hai (pehle "en-IN" hard-code thi,
                  jabke user Urdu bolta hai). */
            val lang = try {
                getSharedPreferences("maya", Context.MODE_PRIVATE)
                    .getString("wake_lang", "en-IN") ?: "en-IN"
            } catch (e: Exception) { "en-IN" }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 6)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            starts++
            report("start", lang + "|" + starts)
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

    fun sukoonOn(): Boolean = try {
        getSharedPreferences("maya", Context.MODE_PRIVATE).getBoolean("sukoon", true)
    } catch (e: Exception) { true }

    fun onHaal(h: String) {
        handler.post {
            if (!running) return@post
            if (h == "BOL_RAHI" || h == "APP_SUN") {
                stopGate()
                try { sr?.cancel() } catch (e: Exception) {}
            } else if (h == "KHALI") {
                restart(300)
            }
        }
    }
}
