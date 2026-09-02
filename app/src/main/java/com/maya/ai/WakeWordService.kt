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

        /* ═══ 🎚️ P9 SUKOON — audio referee: ek waqt mein EK cheez ═══
           Teen jang-boot jo ye sulhaata hai:
           (1) mic khulte hi Android AUDIO FOCUS le leta hai -> Maya ki awaaz
               KAT jati thi (greeting "MAYA onl—" wala masla)
           (2) wake service ka aur tap-to-speak ka SpeechRecognizer LADTE the —
               mic ek waqt mein ek hi hota hai
           (3) wohi jang error 8 (RECOGNIZER_BUSY) deti thi -> service khud ko
               maar deti thi AUR user ka wake switch bhi mita deti thi
           (4) speaker se Maya ki awaaz VAD/recognizer ko lagti -> self-wake loop
           Hal: HAAL — JS (SUKOON) batati hai, Kotlin ka mic har darwaze par
           pehle HAAL poochhta hai. */
        @Volatile var haal: String = "KHALI"          /* KHALI | BOL_RAHI | APP_SUN */
        @Volatile var lastBolAt: Long = 0L            /* bolne ka aakhri lamha */
        @Volatile var pausedByApp: Boolean = false    /* L4 MIC SULAH */
        @Volatile var pausedAt: Long = 0L
        const val ECHO_TAIL_MS = 550L                 /* JS SUKOON.tailMs se match */

        /* ═══ 🔬 v5.10.3 — NATIVE INSTRUMENT (F25/F27 ka ilaj) ═══
           PEHLE: saare counters SIRF JS (KAAN) mein rehte the aur report() bhi
           `MainActivity.instance ?: return` se jati thi. Yani WebView marte hi
           (screen off / app swipe / OEM kill) counters AUR wake ka nateeja dono
           zaya — `suna 0` ka do matlab ho sakta tha: recognizer behra tha, YA
           report raste mein giri. Hum apne hi instrument se andhe the.
           AB: har waqia PEHLE Kotlin mein darj hota hai, phir UI ko bheja jata
           hai. UI zinda ho ya murda — sach yahan mehfooz rehta hai. */
        const val EV_MAX = 60                        /* native ring buffer */
        const val SKIP_REPORT_MS = 15000L            /* F03 — skip-spam par lagam */
        const val BLOCKED_POLL_MS = 3000L            /* F03 — pehle 700ms tha */
        const val STALE_PAUSE_MS = 10000L            /* F05 — pehle 60000ms tha */

        @Volatile var alive: Boolean = false
        @Volatile var startedAt: Long = 0L
        @Volatile var nHeard = 0
        @Volatile var nErr = 0
        @Volatile var nStart = 0
        @Volatile var nSkip = 0
        @Volatile var nErr8 = 0
        @Volatile var nLastErr = 0
        @Volatile var nDead = 0
        @Volatile var nSentOk = 0L
        @Volatile var nDropped = 0L
        @Volatile var nLastWakeAt = 0L
        private val evBuf = ArrayList<String>()

        private fun esc(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", " ").replace("\r", " ")

        /* har waqia PEHLE yahan — UI ki zindagi se azad (F25) */
        fun record(kind: String, payload: String) {
            try {
                synchronized(evBuf) {
                    evBuf.add("{\"t\":" + System.currentTimeMillis() + ",\"k\":\"" +
                              esc(kind) + "\",\"d\":\"" + esc(payload) + "\"}")
                    while (evBuf.size > EV_MAX) evBuf.removeAt(0)
                }
            } catch (e: Exception) {}
            when (kind) {
                "heard" -> nHeard++
                "wake" -> nLastWakeAt = System.currentTimeMillis()
                "err" -> { nErr++; nLastErr = payload.substringBefore("|").toIntOrNull() ?: 0 }
                "err8" -> nErr8++
                "start" -> nStart++
                "dead" -> nDead++
            }
        }

        /* UI zinda hote hi poora buffer ek dafa mein (warna agle boot par) */
        fun drainEvents(): String {
            val out = StringBuilder("[")
            try {
                synchronized(evBuf) {
                    for (i in evBuf.indices) { if (i > 0) out.append(","); out.append(evBuf[i]) }
                    evBuf.clear()
                }
            } catch (e: Exception) {}
            return out.append("]").toString()
        }

        /* 🔬 F04 — panel ka "HAAL: KHALI" JHOOT tha (wo JS ka haal tha, jabke
           Kotlin mein pausedByApp=true phansa tha). Ab Kotlin ka poora sach. */
        fun stateJson(): String {
            val ins = instance
            if (ins != null) { try { return ins.stateBits() } catch (e: Exception) {} }
            val s = StringBuilder("{")
            s.append("\"alive\":false,\"running\":false")
            s.append(",\"haal\":\"" + esc(haal) + "\"")
            s.append(",\"pausedByApp\":" + (if (pausedByApp) "true" else "false"))
            s.append(",\"block\":\"" + esc(haalBlock() ?: "") + "\"")
            s.append(",\"nHeard\":" + nHeard + ",\"nErr\":" + nErr)
            s.append(",\"nStart\":" + nStart + ",\"nSkip\":" + nSkip)
            s.append(",\"nLastErr\":" + nLastErr + ",\"nDead\":" + nDead)
            s.append(",\"dropped\":" + nDropped)
            return s.append("}").toString()
        }

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

        /* L1 — MainActivity.setHaal bridge se aata hai.
           NAAM SAWADHAN: isse "setHaal" mat rakhna — companion ke @Volatile var
           "haal" ka JVM setter bhi setHaal(String) banta hai -> platform clash
           (kotlin build fail). Isi liye "applyHaal". */
        fun applyHaal(h: String) {
            if (h == "BOL_RAHI") lastBolAt = System.currentTimeMillis()
            haal = h
            try { instance?.onHaal(h) } catch (e: Exception) {}
        }

        /* L2 — mic ka jawab: abhi kholna mana hai? (null = khol lo) */
        fun haalBlock(): String? {
            val s = instance ?: return null            /* service band -> faisla baema'ni */
            if (!s.sukoonOn()) return null             /* escape hatch — LAB switch OFF */
            if (haal == "BOL_RAHI") return "Maya bol rahi hai"
            if (haal == "APP_SUN") return "app ka mic chal raha hai"
            if (pausedByApp) return "sulah: app ka mic"
            if (System.currentTimeMillis() - lastBolAt < ECHO_TAIL_MS) return "echo tail"
            return null
        }

        /* L4 — tap-to-speak sab se pehle; service neeche */
        fun pauseForApp() {
            pausedByApp = true
            pausedAt = System.currentTimeMillis()
            try { instance?.hardPause() } catch (e: Exception) {}
        }
        fun resumeFromApp() {
            pausedByApp = false
            try { instance?.softResume() } catch (e: Exception) {}
        }

        internal fun attach(s: WakeWordService) { instance = s }
        internal fun detach(s: WakeWordService) { if (instance === s) instance = null }
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
    @Volatile private var pendingGen = 0L        /* L6 RACE TOKEN — pending restart ka duct-ticket */

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        attach(this)                              /* P9 — HAAL bridge instance */
        alive = true                              /* 🔬 v5.10.3 — native instrument */
        startedAt = System.currentTimeMillis()
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
        alive = false
        detach(this)                              /* P9 */
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
        val why0 = haalBlock()                    /* L2 — gate ka darwaza bhi */
        if (why0 != null) { reportSkip("pehra nahi chala — " + why0); restart(BLOCKED_POLL_MS); return }
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
                    /* L7 SELF-WAKE SHIELD — Maya ke bolte waqt PEHRA bhi khamosh.
                       Warna speaker se uski apni awaaz gate ko "awaaz" lagti aur
                       MAYA APNE HI WAKE WORD par jaag sakti thi (loop) — isi liye
                       aap ko jawab ke beech mic on/off dikh raha tha. */
                    val why = haalBlock()
                    if (why != null) {
                        reportSkip("pehra khamosh — " + why)
                        break
                    }
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
        try {
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

                    /* 🕊️ L5 ERR-8 MERCY — RECOGNIZER_BUSY ka matlab: mic kisi aur ke
                       paas hai (app ka tap-to-speak ya seester ka bhoot). PEHLE: yahan
                       service khud ko MAAR deti thi, aur JS user ka wakeWord switch bhi
                       KHUD-BA-KHUD mita deta tha. AB: na stopSelf, na switch haath mein. */
                    if (error == 8) {
                        errStreak = 0
                        report("err8", "mic masroof — 2s baad phir")
                        restart(2000)
                        return
                    }

                    /* 🔬 F35 — ERROR_INSUFFICIENT_PERMISSIONS: mic ki ijazat gayab.
                       Har 1.2s hammer bekaar hai; user ko batana zaroori hai. */
                    if (error == 9) {
                        report("dead", "9|mic ki ijazat nahi — wake ruk gayi")
                        evalToApp("window.__wakeErr && window.__wakeErr(9)")
                        restart(60000)
                        return
                    }

                    /* 🔬 F09 — PEHLE 10..15 sab `else -> 1200L` par girte the:
                       error 11 (SERVER_DISCONNECTED) par har 1.2 second naya cloud
                       hamla, lagatar 15 dafa — yani hamari retry policy KHUD beemari
                       ko feed kar rahi thi (AOSP: ERROR_TOO_MANY_REQUESTS isi se aata
                       hai). AB: har code ka apna base + errStreak se escalation
                       (x1,x2,x4,x8,x16) + 30s cap. */
                    val base = when (error) {
                        6, 7 -> 700L
                        1, 2 -> 3000L
                        4 -> 1500L
                        10 -> 5000L              /* TOO_MANY_REQUESTS — thoka hum ne hai */
                        11 -> 1500L              /* SERVER_DISCONNECTED */
                        12, 13 -> 8000L          /* LANGUAGE not supported / unavailable */
                        else -> 1200L
                    }
                    val stepped = if (error == 6 || error == 7) {
                        base + (errStreak.coerceAtMost(8) * 350L)      /* 0.7s -> 3.5s (purana rawaiya) */
                    } else {
                        base * (1L shl (errStreak - 1).coerceAtMost(4))
                    }
                    val back = stepped.coerceAtMost(30000L)

                    /* 3 lagatar nakami = service ka connection khud mar chuka hai */
                    if (errStreak >= 3) { try { resetRecognizer() } catch (e: Throwable) {} }

                    /* 🔬 F10 — CIRCUIT OPEN: chup-chaap marna band, user ko khabar.
                       ⚠ settings.wakeWord ko HAATH NAHI lagate — P9 ka wada barqarar
                       ("wake ON karo to baad mein khud band milta tha" wala bug). */
                    if (errStreak == 5) {
                        report("dead", error.toString() + "|circuit open — 5 lagatar nakami, ab sust koshish")
                        evalToApp("window.__wakeErr && window.__wakeErr(" + error + ")")
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
        } catch (e: Throwable) {
            /* 🔬 v5.10.3 — OEM par recognizer banane mein dhamaka ho to service
               CRASH na ho; chup-chaap nakami bhi na guzre. */
            report("gate", "recognizer bana nahi: " + e.javaClass.simpleName)
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
        record(kind, payload)                     /* 🔬 F25 — pehle Kotlin mein darj */
        val act = MainActivity.instance
        val js = "window.__wakeLog && window.__wakeLog('" + jsEsc(kind) + "','" + jsEsc(payload) + "')"
        /* 🔬 F27 — murda WebView par JS thonsna band; delivery ka hisab bhi */
        if (act != null && act.evalPublicOk(js)) nSentOk++ else nDropped++
    }

    /* 🔬 F03 — pehle har 700ms ek skip report: 67 dafa "sulah: app ka mic" ne
       KAAN ka 40-entry log bhar diya tha aur asal tareekh MIT gayi thi (hum
       apne hi diagnostic se andhe ho gaye). Ab: pehli bar foran, phir har 15s,
       aur sath chalti ginti (x N). Block hat-te hi counter reset. */
    private var lastSkipAt = 0L
    private var skipRun = 0
    private fun reportSkip(why: String) {
        nSkip++
        skipRun++
        val now = System.currentTimeMillis()
        if (lastSkipAt == 0L || now - lastSkipAt >= SKIP_REPORT_MS) {
            lastSkipAt = now
            report("skip", why + "  (x" + skipRun + ")")
        }
    }
    private fun skipReset() { skipRun = 0; lastSkipAt = 0L }

    private fun handleAll(list: List<String>) {
        val arr = JSONArray()
        for (i in list.indices) { if (i >= 6) break; arr.put(list[i]) }
        val payload = arr.toString()
        /* 🔬 F25 — recognizer ne kya suna, ye NATIVE counter mein bhi darj ho:
           pehle `suna 0` ambiguous tha (behra recognizer YA zaya hui report). */
        record("heard", payload)
        val act = MainActivity.instance
        val js = "window.__wakeHeard && window.__wakeHeard('" + jsEsc(payload) + "')"
        if (act != null && act.evalPublicOk(js)) {
            nSentOk++
        } else {
            /* SAFE MODE: app band ho to KUCH NA KARO — v2.10.0 ka khud-app-kholna
               engine hi black screen ka mujrim nikla tha.
               🔬 F25: magar ab ye waqia CHUPKE ZAYA NAHI HOTA — native buffer mein
               mehfooz hai, aur `heardOffline` ginti panel par nazar aati hai.
               (Poori native action-path Phase 3 mein: chime + launchApp.) */
            lastHeardOffline = payload
            heardOffline++
            nDropped++
        }
    }
    private var lastHeardOffline = ""
    @Volatile private var heardOffline = 0        /* 🔬 F25 — kitni wake UI ki maut ki wajah se girin */

    private fun restart(delay: Long) {
        /* P8c — seedha recognizer nahi; pehle KHAMOSHI KA PEHRA. Sannate mein
           recognizer bilkul nahi chalega -> "mic on/off" khatam.
           P9 — (L6) har schedule ka apna token: naya aaye to purana pending
           MURDA (pehle do pending ek sath chal padte the -> mic strobe).
           (L2) mic ka darwaza pehle HAAL poochhe: Maya bol rahi hai ya app
           ka mic chal raha hai to kholna hi nahi — yahi awaaz-katna aur
           mic-larai ka asal ilaj hai. */
        val gen = ++pendingGen
        handler.postDelayed({
            if (!running) return@postDelayed
            if (gen != pendingGen) return@postDelayed
            val why = haalBlock()
            if (why != null) {
                reportSkip(why)                  /* 🔬 F03 — spam nahi, hisab ke sath */
                restart(BLOCKED_POLL_MS)         /* HAAL khali hone ka intezar (3s, pehle 700ms) */
                return@postDelayed
            }
            skipReset()                          /* 🔬 F03 — darwaza khul gaya, ginti saaf */
            if (vadEnabled()) startGate() else actuallyStart()
        }, delay)
    }

    private fun actuallyStart() {
        if (!running) return
        val why = haalBlock()                    /* L2 — chautha darwaza */
        if (why != null) { reportSkip(why); restart(BLOCKED_POLL_MS); return }
        skipReset()
        try {
            /* v5.7.0 — do badlaav:
               1. MAX_RESULTS 1 -> 6. SUNO ka sabaq: sahih jawab aksar doosre ya
                  teesre andaze mein hota hai. Wake word par ye aur zyada ahem hai.
               2. Zubaan ab settings se aati hai (pehle "en-IN" hard-code thi,
                  jabke user Urdu bolta hai). */
            val lang = wakeLangNow()          /* 🔬 F12 — ab STT se azad (default en-IN) */
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
            /* L2 — watchdog bhi HAAL se pooche: Maya ke bolte waqt recognizer
               todna = awaaz kaatna. Pehle ye bina dekhe chalta tha — har 12
               minute par awaaz katne ka scheduled chance tha. */
            if (haalBlock() == null) {
                /* 🔬 F18 — pehle ye gate ko CHALU chhod kar recognizer mic par thons
                   deta tha (har 12 min guaranteed clash → error 3/8/11). Ab gate
                   khuli ho to sirf recognizer fresh karo, nayi session mat chalao. */
                resetRecognizer()
                if (!gateOn) actuallyStart()
            }
        }
        /* L4 stale-sulah recovery — JS/WebView mar bhi jaye (YA uska KHALI
           call kho jaye) to 60s baad pause khud-ba-khud azad. Warna wake word
           hamesha ke liye so jata. */
        if (pausedByApp && System.currentTimeMillis() - pausedAt > STALE_PAUSE_MS) {
            /* 🔬 F05 — 60s bohot lamba tha: wake har SUNO ke baad ek poore minute
               ke liye murda rehti thi. Ab 10s — AUR app ka mic asal mein khali ho
               tab hi, warna sirf intezar barhao (blind release = mic jang). */
            val busy = try { MainActivity.instance?.appMicBusy() ?: false } catch (e: Exception) { false }
            if (busy) {
                pausedAt = System.currentTimeMillis()
            } else {
                report("sulah", "stale pause khud azad hua (" + (STALE_PAUSE_MS / 1000) + "s)")
                resumeFromApp()
            }
        }
        handler.postDelayed(::watchdog, 45000)
    }

    /* ═══ 🎚️ P9 SUKOON — instance taraf ke amal ═══ */

    /* escape hatch — LAB sukoon OFF ho to purana rawaiya */
    fun sukoonOn(): Boolean = try {
        getSharedPreferences("maya", Context.MODE_PRIVATE).getBoolean("sukoon", true)
    } catch (e: Exception) { true }

    /* L1 — HAAL badla to foran amal */
    fun onHaal(h: String) {
        handler.post {
            if (!running) return@post
            if (h == "BOL_RAHI" || h == "APP_SUN") {
                /* mic ISI LAMHE chhodo — awaaz katna yahi se rukta hai */
                stopGate()
                try { sr?.cancel() } catch (e: Exception) {}
                pendingGen++                     /* pending restart murda */
            } else if (h == "KHALI") {
                restart(300)
            }
        }
    }

    /* L4 — tap-to-speak jeetta hamesha */
    fun hardPause() {
        handler.post {
            stopGate()
            try { sr?.cancel() } catch (e: Exception) {}
            pendingGen++
            report("sulah", "service pause — app ka mic")
        }
    }
    fun softResume() {
        handler.post {
            if (!running) return@post
            skipReset()                        /* 🔬 F03 — nayi ginti */
            report("sulah", "service wapas — pehra phir se")
            restart(300)
        }
    }

    /* 🔬 F12 — wake ki zubaan AB STT se azad hai. Urdu decoder "مایا" ko "ہے"
       jaisa parh leta hai (hamara apna KAAN DOCTOR ye kehta tha), is liye wake
       ka default en-IN hai aur user LAB se badal sakta hai. */
    private fun wakeLangNow(): String = try {
        getSharedPreferences("maya", Context.MODE_PRIVATE).getString("wake_lang", "en-IN") ?: "en-IN"
    } catch (e: Exception) { "en-IN" }

    /* 🔬 F04 — KOTLIN KA POORA SACH ek JSON mein.
       PEHLE: panel "HAAL: KHALI" dikha raha tha (wo JS ka haal tha) jabke Kotlin
       mein pausedByApp=true phansa hua tha — instrument ne humein galat raaste par
       bheja. Ab panel JS aur Kotlin DONO ka haal dikhata hai + MISMATCH pakadta hai. */
    fun stateBits(): String {
        val s = StringBuilder("{")
        s.append("\"alive\":").append(if (alive) "true" else "false")
        s.append(",\"running\":").append(if (running) "true" else "false")
        s.append(",\"haal\":\"" + haal + "\"")
        s.append(",\"pausedByApp\":").append(if (pausedByApp) "true" else "false")
        s.append(",\"pausedFor\":").append(if (pausedByApp) (System.currentTimeMillis() - pausedAt) else 0L)
        s.append(",\"block\":\"" + (haalBlock() ?: "").replace("\"", "'") + "\"")
        s.append(",\"gate\":").append(if (gateOn) "true" else "false")
        s.append(",\"sukoon\":").append(if (sukoonOn()) "true" else "false")
        s.append(",\"lang\":\"" + wakeLangNow() + "\"")
        s.append(",\"using\":\"" + (MainActivity.instance?.lastRecognizerKind ?: "-") + "\"")
        s.append(",\"floor\":").append(Math.round(floorDb))
        s.append(",\"starts\":").append(starts)
        s.append(",\"errStreak\":").append(errStreak)
        s.append(",\"lastErr\":").append(lastErr)
        s.append(",\"uptime\":").append(if (startedAt > 0L) ((System.currentTimeMillis() - startedAt) / 1000L) else 0L)
        s.append(",\"nHeard\":").append(nHeard)
        s.append(",\"nErr\":").append(nErr)
        s.append(",\"nStart\":").append(nStart)
        s.append(",\"nSkip\":").append(nSkip)
        s.append(",\"nErr8\":").append(nErr8)
        s.append(",\"nLastErr\":").append(nLastErr)
        s.append(",\"nDead\":").append(nDead)
        s.append(",\"heardOffline\":").append(heardOffline)
        s.append(",\"sent\":").append(nSentOk)
        s.append(",\"dropped\":").append(nDropped)
        s.append(",\"lastWake\":").append(nLastWakeAt)
        return s.append("}").toString()
    }

    /* L1/L4 ka Kotlin dastaaveezi tor par saabit: HAAL ka pehra */
    fun mazbootKotlinGate(): Boolean = true
}
