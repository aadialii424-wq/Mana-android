package com.maya.ai

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.app.Activity
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.provider.Settings
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.AlarmClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import java.util.Locale

/**
 * MAYA — Personal AI (Phase 2: Native APK)
 * WebView shell + native bridge:
 *  - Native STT (SpeechRecognizer — real Urdu/Hindi/English voice input)
 *  - Native TTS (system engine — proper Urdu voice support)
 *  - REAL system alarms & timers (AlarmClock intents)
 *  - Notifications, vibration, battery status, keep-screen-on
 */
class MainActivity : AppCompatActivity() {

    companion object {
        const val VIRTUAL_HOST = "appassets.androidplatform.net"
        const val CHANNEL_ID = "maya_notifications"
        const val REQ_PERMS = 7001
        var instance: MainActivity? = null
    }

    private lateinit var webView: WebView
    private lateinit var assetLoader: WebViewAssetLoader
    @Volatile private var webViewAlive = false
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var recognizer: SpeechRecognizer? = null

    /* ================= LIFECYCLE ================= */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this)
        webView.setBackgroundColor(0xFF050B14.toInt())
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowUniversalAccessFromFileURLs = true
            allowFileAccessFromFileURLs = true
        }
        webView.addJavascriptInterface(MayaBridge(), "MayaBridge")
        webView.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onConsoleMessage(msg: android.webkit.ConsoleMessage): Boolean {
                val t = msg.message() ?: ""
                if (msg.messageLevel() == android.webkit.ConsoleMessage.MessageLevel.ERROR && t.isNotBlank()) {
                    evalAsync("window.__consoleErr && window.__consoleErr('" +
                        t.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ").replace("\r", " ") + "')")
                }
                return true
            }
        }
        webView.webViewClient = MayaWebViewClient()
        setContentView(webView)
        webView.loadUrl("https://$VIRTUAL_HOST/assets/web/index.html")
        Toast.makeText(this, "MAYA v2.11.1 • nayi build install hui hai", Toast.LENGTH_LONG).show()
        // WebView zinda hai ya nahi — 8 second baad native check
        webViewAlive = false
        android.os.Handler(Looper.getMainLooper()).postDelayed({
            if (!webViewAlive) {
                Toast.makeText(this, "WebView load NAHI hua (blank ka wajah) — developer ko batayen", Toast.LENGTH_LONG).show()
            }
        }, 8000)

        initTts()
        createNotificationChannel()
        requestNeededPermissions()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        try {
            var bytes: ByteArray? = null
            if (requestCode == 5001) {
                val f = java.io.File(cacheDir, "maya_photo.jpg")
                if (f.exists()) bytes = f.readBytes()
            } else if (requestCode == 5002 && data?.data != null) {
                contentResolver.openInputStream(data.data!!)?.use { it.readBytes() }?.let { bytes = it }
            }
            if (bytes != null && bytes!!.size in 1..4_000_000) {
                val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                evalAsync("window.__photoTaken && window.__photoTaken('" + b64 + "')")
            }
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        instance = null
        stopRecognizer()
        try { tts?.stop(); tts?.shutdown() } catch (e: Exception) {}
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    /* ================= WEBVIEW CLIENT ================= */

    inner class MayaWebViewClient : WebViewClientCompat() {
        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest
        ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean {
            val url = request.url
            if (url.host == VIRTUAL_HOST) return false   // apni app — andar khule
            return try {
                startActivity(Intent(Intent.ACTION_VIEW, url))  // bahar ke links/apps
                true
            } catch (e: ActivityNotFoundException) {
                val fb = when (url.scheme) {
                    "instagram" -> "https://www.instagram.com/"
                    "market" -> "https://play.google.com/store/apps"
                    "fb" -> "https://m.facebook.com/"
                    "tg" -> "https://web.telegram.org/"
                    "googlegmail" -> "https://mail.google.com/"
                    "nflx" -> "https://www.netflix.com/"
                    "spotify" -> "https://open.spotify.com/"
                    "whatsapp" -> "https://web.whatsapp.com/"
                    "geo" -> "https://maps.google.com/"
                    else -> null
                }
                if (fb != null) {
                    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fb))) }
                    catch (x: Exception) { toast("Ye app is phone par install nahi hai") }
                } else toast("Ye app/link is phone par nahi mila")
                true
            }
        }
    }

    /* ================= TTS ================= */

    private fun initTts() {
        tts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == "maya")
                        evalAsync("window.__nativeTtsDone && window.__nativeTtsDone()")
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (utteranceId == "maya")
                        evalAsync("window.__nativeTtsDone && window.__nativeTtsDone()")
                }
            })
        }
    }

    /* ================= STT ================= */

    private fun stopRecognizer() {
        try { recognizer?.destroy(); recognizer = null } catch (e: Exception) {}
    }

    /* ================= JS BRIDGE ================= */

    inner class MayaBridge {

        @JavascriptInterface
        fun appVersion(): String = "2.1.0-native"

        /** Native TTS v2 — voice picker + pitch (crispy awaaz) */
        @JavascriptInterface
        fun speak(text: String, lang: String, rate: Double, pitch: Double, voiceName: String) {
            runOnUiThread {
                if (!ttsReady) {
                    evalAsync("window.__nativeTtsDone && window.__nativeTtsDone()")
                    return@runOnUiThread
                }
                try {
                    val engine = tts ?: return@runOnUiThread
                    try {
                        if (voiceName.isNotEmpty()) {
                            engine.voices?.firstOrNull { it.name == voiceName }?.let { engine.setVoice(it) }
                        } else {
                            val loc = Locale.forLanguageTag(lang.replace('_', '-'))
                            val res = engine.setLanguage(loc)
                            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                                engine.language = Locale.getDefault()
                            }
                        }
                    } catch (e: Exception) {}
                    engine.setSpeechRate(rate.toFloat().coerceIn(0.5f, 2f))
                    engine.setPitch(pitch.toFloat().coerceIn(0.5f, 2f))
                    engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "maya")
                } catch (e: Exception) {
                    evalAsync("window.__nativeTtsDone && window.__nativeTtsDone()")
                }
            }
        }

        /** Phone ki saari TTS voices ki list (JS ke liye JSON) */
        @JavascriptInterface
        fun ttsVoices(): String {
            return try {
                val arr = JSONArray()
                tts?.voices?.forEach { v ->
                    arr.put(
                        JSONObject()
                            .put("name", v.name)
                            .put("locale", v.locale.toLanguageTag())
                            .put("network", v.isNetworkConnectionRequired)
                    )
                }
                arr.toString()
            } catch (e: Exception) { "[]" }
        }

        @JavascriptInterface
        fun stopSpeak() {
            runOnUiThread { try { tts?.stop() } catch (e: Exception) {} }
        }

        /** Native STT — Google voice recognition (Urdu ur-PK supported) */
        @JavascriptInterface
        fun listen(lang: String) {
            runOnUiThread {
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity, Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestMicPermission()
                    evalAsync("window.__nativeSpeechErr && window.__nativeSpeechErr(7)")
                    return@runOnUiThread
                }
                if (!SpeechRecognizer.isRecognitionAvailable(this@MainActivity)) {
                    evalAsync("window.__nativeSpeechErr && window.__nativeSpeechErr(5)")
                    return@runOnUiThread
                }
                stopRecognizer()
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 700)
                }
                recognizer = SpeechRecognizer.createSpeechRecognizer(this@MainActivity).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        private var rmsTick = 0
                        override fun onRmsChanged(rmsdB: Float) {
                            rmsTick++
                            if (rmsTick % 4 == 0) evalAsync("window.__nativeRms && window.__nativeRms(" + rmsdB + ")")
                        }
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() { evalAsync("window.__nativePartial && window.__nativePartial('')") }
                        override fun onError(error: Int) {
                            evalAsync("window.__nativeSpeechErr && window.__nativeSpeechErr($error)")
                        }
                        override fun onResults(results: Bundle?) {
                            val text = results
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull() ?: ""
                            evalAsync("window.__nativeSpeech && window.__nativeSpeech('" + jsEscape(text) + "')")
                        }
                        override fun onPartialResults(partialResults: Bundle?) {
                            val pt = partialResults
                                ?.getStringArrayList("android.speech.extra.RESULTS")?.firstOrNull() ?: ""
                            if (pt.isNotBlank()) evalAsync("window.__nativePartial && window.__nativePartial('" + jsEscape(pt) + "')")
                        }
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                    startListening(intent)
                }
            }
        }

        @JavascriptInterface
        fun stopListen() {
            runOnUiThread { stopRecognizer() }
        }

        /** REAL alarm v2 — 3-layer: silent set > prefilled UI > fail */
        @JavascriptInterface
        fun setAlarm(hour: Int, minute: Int, message: String): Int {
            val msg = message.ifEmpty { "MAYA Alarm" }
            try {
                val i = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_MESSAGE, msg)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                }
                startActivity(i)
                return 2
            } catch (e: Exception) {}
            try {
                val i = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_MESSAGE, msg)
                }
                startActivity(i)
                return 1
            } catch (e: Exception) {}
            return 0
        }

        /** REAL system timer — screen band ho to bhi bajta hai */
        @JavascriptInterface
        fun setTimer(seconds: Int, message: String): Boolean {
            return try {
                val i = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                    putExtra(AlarmClock.EXTRA_MESSAGE, message.ifEmpty { "MAYA Timer" })
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                }
                startActivity(i)
                true
            } catch (e: Exception) { false }
        }

        /** Battery status (JSON string — sync return) */
        @JavascriptInterface
        fun battery(): String {
            return try {
                val b = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    ?: return "{}"
                val level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                val status = b.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                JSONObject()
                    .put("level", if (level >= 0) level * 100 / scale else -1)
                    .put(
                        "charging",
                        status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL
                    )
                    .toString()
            } catch (e: Exception) { "{}" }
        }

        @JavascriptInterface
        fun vibrate(ms: Long) {
            try {
                val vib: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION") vib.vibrate(ms)
                }
            } catch (e: Exception) {}
        }

        @JavascriptInterface
        fun notify(title: String, text: String) {
            runOnUiThread {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestNotificationPermission()
                        return@runOnUiThread
                    }
                    val pi = PendingIntent.getActivity(
                        this@MainActivity, 0,
                        Intent(this@MainActivity, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val n = NotificationCompat.Builder(this@MainActivity, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setAutoCancel(true)
                        .setContentIntent(pi)
                        .build()
                    getSystemService(NotificationManager::class.java)
                        .notify((System.currentTimeMillis() % 10000).toInt(), n)
                } catch (e: Exception) {}
            }
        }

        /** Wake word service — background mein 'Maya'/'Boss' sunti hai */
        @JavascriptInterface
        fun wakeService(start: Boolean): Boolean {
            return try {
                if (start) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                        requestMicPermission()
                        return false
                    }
                    WakeWordService.start(this@MainActivity)
                    prefs().edit().putBoolean("wake", true).apply()
                    true
                } else {
                    WakeWordService.stop(this@MainActivity)
                    prefs().edit().putBoolean("wake", false).apply()
                    true
                }
            } catch (e: Exception) { false }
        }

        /** YouTube v2 — innertube JSON + consent cookie fallback (pakka videoId) */
        @JavascriptInterface
        fun ytSearch(query: String): String {
            // 1) Innertube ANDROID client — JSON, reliable
            try {
                val conn = URL("https://www.youtube.com/youtubei/v1/search?prettyPrint=false")
                    .openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("User-Agent", "com.google.android.youtube/20.10.38 (Linux; U; Android 11) gzip")
                val esc = query.replace("\\", "\\\\").replace("\"", "\\\"")
                val body = "{\"context\":{\"client\":{\"clientName\":\"ANDROID\",\"clientVersion\":\"20.10.38\",\"androidSdkVersion\":30,\"hl\":\"en\",\"gl\":\"US\"}},\"query\":\"" + esc + "\"}"
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val txt = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val m = Regex("\"videoId\":\"([a-zA-Z0-9_-]{11})\"").find(txt)
                if (m != null) return m.groupValues[1]
            } catch (e: Exception) {}
            // 2) HTML + CONSENT cookie
            try {
                val conn = URL("https://www.youtube.com/results?search_query=" + URLEncoder.encode(query, "UTF-8"))
                    .openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36")
                conn.setRequestProperty("Cookie", "CONSENT=YES+cb.20240101-01-p0.en+FX+000; SOCS=CAI")
                conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                val html = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val m = Regex("\"videoId\":\"([a-zA-Z0-9_-]{11})\"").find(html)
                if (m != null) return m.groupValues[1]
            } catch (e: Exception) {}
            return ""
        }

        /** CONTACTS ENGINE (Phase 5) */
        @JavascriptInterface
        fun contactsSearch(query: String): String {
            return try {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_CONTACTS)
                    != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.READ_CONTACTS), REQ_PERMS)
                    return "{\"error\":\"permission\"}"
                }
                val q = query.trim()
                if (q.isEmpty()) return "{\"matches\":[]}"
                val arr = JSONArray()
                val seen = HashSet<String>()
                // naam se contact
                val cur = contentResolver.query(
                    ContactsContract.Contacts.CONTENT_FILTER_URI.buildUpon().appendPath(q).build(),
                    arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
                    null, null, null
                )
                cur?.use { c ->
                    while (c.moveToNext() && arr.length() < 6) {
                        val id = c.getString(0) ?: continue
                        val name = c.getString(1) ?: continue
                        val pcur = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                            arrayOf(id), null
                        )
                        pcur?.use { p ->
                            while (p.moveToNext() && arr.length() < 6) {
                                val num = p.getString(0) ?: continue
                                if (seen.add(name + "|" + num)) {
                                    arr.put(JSONObject().put("name", name).put("number", num))
                                }
                            }
                        }
                    }
                }
                // dialpad/number se bhi
                if (arr.length() == 0) {
                    val pcur = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI.buildUpon().appendPath(q).build(),
                        arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                ContactsContract.CommonDataKinds.Phone.NUMBER),
                        null, null, null
                    )
                    pcur?.use { p ->
                        while (p.moveToNext() && arr.length() < 6) {
                            val name = p.getString(0) ?: continue
                            val num = p.getString(1) ?: continue
                            if (seen.add(name + "|" + num)) {
                                arr.put(JSONObject().put("name", name).put("number", num))
                            }
                        }
                    }
                }
                JSONObject().put("matches", arr).toString()
            } catch (e: Exception) { "{\"error\":\"" + (e.message ?: "x") + "\"}" }
        }

        /** Naam se seedha CALL (bina tap — ACTION_CALL) */
        @JavascriptInterface
        fun autoCall(number: String): Boolean {
            return try {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CALL_PHONE)
                    != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.CALL_PHONE), REQ_PERMS)
                    return false
                }
                startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number)))
                true
            } catch (e: Exception) { false }
            }

        /** WhatsApp: number + message draft (PK normalization + auto-send flag) */
        @JavascriptInterface
        fun openWhatsAppDraft(number: String, text: String, autoSend: Boolean): Boolean {
            return try {
                var n = number.replace(Regex("[^\\d]"), "")
                if (n.startsWith("00")) n = n.substring(2)
                if (n.startsWith("0") && n.length in 10..11) n = "92" + n.substring(1)
                if (autoSend) prefs().edit().putLong("autosend_at", System.currentTimeMillis()).apply()
                val u = "https://wa.me/" + n + "?text=" + URLEncoder.encode(text, "UTF-8")
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
                true
            } catch (e: Exception) { false }
        }

        /** SMS draft (number + text) */
        @JavascriptInterface
        fun smsDraft(number: String, text: String): Boolean {
            return try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                    "sms:" + number + "?body=" + URLEncoder.encode(text, "UTF-8"))))
                true
            } catch (e: Exception) { false }
        }

        /** Battery shield — unrestricted (background mic kill se bachao) */
        @SuppressLint("BatteryLife")
        @JavascriptInterface
        fun requestBatteryUnrestricted(): Boolean {
            return try {
                val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                if (pm.isIgnoringBatteryOptimizations(packageName)) return true
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + packageName)))
                true
            } catch (e: Exception) { false }
        }

        @JavascriptInterface
        fun batteryUnrestricted(): Boolean {
            return try {
                val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                pm.isIgnoringBatteryOptimizations(packageName)
            } catch (e: Exception) { false }
        }

        /** AutoSend accessibility status + settings kholna */
        @JavascriptInterface
        fun accessibilityEnabled(): Boolean {
            return try {
                val s = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                    ?: return false
                s.contains(".AutoSendService")
            } catch (e: Exception) { false }
        }

        @JavascriptInterface
        fun deviceBrand(): String = Build.MANUFACTURER ?: "unknown"

        @JavascriptInterface
        fun openAppDetails(): Boolean {
            return try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + packageName)))
                true
            } catch (e: Exception) { false }
        }

        @JavascriptInterface
        fun openAccessibilitySettings(): Boolean {
            return try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                true
            } catch (e: Exception) { false }
        }

        /** FILE MANAGER (Phase 8) — list/open/share */
        @JavascriptInterface
        fun listFiles(folder: String): String {
            return try {
                val f = folder.lowercase().trim()
                val uri: Uri = when (f) {
                    "pictures", "photos" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "music", "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    "videos", "movies" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    else MediaStore.Files.getContentUri("external")
                }
                val arr = JSONArray()
                val proj = arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.MIME_TYPE,
                    MediaStore.MediaColumns.SIZE
                )
                val cur = contentResolver.query(uri, proj, null, null, MediaStore.MediaColumns.DATE_MODIFIED + " DESC")
                cur?.use { c ->
                    var i = 0
                    while (c.moveToNext() && i < 40) {
                        val id = c.getLong(0)
                        val name = c.getString(1) ?: continue
                        val mime = c.getString(2) ?: ""
                        val size = c.getLong(3)
                        val itemUri = Uri.withAppendedPath(uri, id.toString())
                        arr.put(JSONObject()
                            .put("name", name)
                            .put("type", mime)
                            .put("size", size)
                            .put("uri", itemUri.toString()))
                        i++
                    }
                }
                JSONObject().put("files", arr).put("folder", f).toString()
            } catch (e: Exception) { JSONObject().put("error", e.message ?: "x").toString() }
        }

        @JavascriptInterface
        fun openFile(uriStr: String, mime: String): Boolean {
            return try {
                val i = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    if (mime.isNotEmpty()) setDataAndType(Uri.parse(uriStr), mime)
                }
                startActivity(i)
                true
            } catch (e: Exception) { false }
        }

        @JavascriptInterface
        fun shareFile(uriStr: String, mime: String): Boolean {
            return try {
                val i = Intent(Intent.ACTION_SEND).apply {
                    type = mime.ifEmpty { "*/*" }
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(uriStr))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(i, "MAYA share"))
                true
            } catch (e: Exception) { false }
        }

        @JavascriptInterface
        fun requestFilesPerms(): Boolean {
            return try {
                val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
                else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                ActivityCompat.requestPermissions(this@MainActivity, perms, REQ_PERMS)
                true
            } catch (e: Exception) { false }
        }

        /* ===== QUICK CONTROLS (Phase 9) ===== */
        @JavascriptInterface
        fun torch(on: Boolean): Boolean {
            return try {
                val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val id = cm.cameraIdList.firstOrNull { cid ->
                    cm.getCameraCharacteristics(cid)
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                } ?: return false
                cm.setTorchMode(id, on)
                true
            } catch (e: Exception) { false }
        }

        @JavascriptInterface
        fun volume(pct: Int): Int {
            return try {
                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val v = (max * pct.coerceIn(0, 100)) / 100
                am.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0)
                v * 100 / max
            } catch (e: Exception) { -1 }
        }

        @JavascriptInterface
        fun brightness(pct: Int): Int {
            return try {
                if (!Settings.System.canWrite(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:" + packageName)))
                    return -2
                }
                val max = 255
                val b = (max * pct.coerceIn(5, 100)) / 100
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, b)
                b * 100 / max
            } catch (e: Exception) { -1 }
        }

        @JavascriptInterface
        fun lockScreen(): Boolean {
            return try {
                val svc = com.maya.ai.AutoSendService.instance
                svc?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) == true
            } catch (e: Exception) { false }
        }

        @JavascriptInterface
        fun scheduleTask(id: String, delayMs: Long): Boolean =
            try { com.maya.ai.ScheduledReceiver.schedule(this@MainActivity, id, delayMs) } catch (e: Exception) { false }

        /* ===== WHATSAPP READER ===== */
        @JavascriptInterface
        fun notifHistory(): String =
            try { com.maya.ai.MayaNotifService.historyJson() } catch (e: Exception) { "[]" }

        @JavascriptInterface
        fun notifClear() { try { com.maya.ai.MayaNotifService.clear() } catch (e: Exception) {} }

        @JavascriptInterface
        fun notifSpeak(on: Boolean) { com.maya.ai.MayaNotifService.speakOn = on }

        @JavascriptInterface
        fun notifEnabled(): Boolean {
            return try {
                val s = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
                s.contains(packageName)
            } catch (e: Exception) { false }
        }

        @JavascriptInterface
        fun openNotifAccess(): Boolean {
            return try { startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")); true }
            catch (e: Exception) { false }
        }

        /* ===== REPLY via notification action (asli auto-reply) ===== */
        @JavascriptInterface
        fun notifReply(fromName: String, text: String): Int {
            return try {
                val nb = com.maya.ai.MayaNotifService.buffer
                val target = synchronized(nb) {
                    nb.toList().lastOrNull { it.optString("from") == fromName && it.optBoolean("canReply") }
                } ?: return -1
                // dobara live notification se action lo (posted list se)
                val sbns = this@MainActivity.let { _ ->
                    // listener instance ke through activeNotifications nahi milta yahan se,
                    // to buffer wala pendingIntent nahi hota — is liye reply sirf tab jab
                    // listener attached ho; hum notif list scan nahi kar sakte activity se.
                    null
                }
                // Simple robust raasta: WhatsApp draft kholo (auto-send ke saath)
                val okDraft = openWhatsAppDraftLookup(fromName, text)
                if (okDraft) 1 else 0
            } catch (e: Exception) { 0 }
        }

        private fun openWhatsAppDraftLookup(fromName: String, text: String): Boolean {
            return try {
                // contact se number dhoondo aur draft + autosend kholo
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_CONTACTS)
                    == PackageManager.PERMISSION_GRANTED) {
                    val cur = contentResolver.query(
                        ContactsContract.Contacts.CONTENT_FILTER_URI.buildUpon().appendPath(fromName).build(),
                        arrayOf(ContactsContract.Contacts._ID), null, null, null)
                    var number: String? = null
                    cur?.use { c ->
                        if (c.moveToNext()) {
                            val id = c.getString(0)
                            val pc = contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                                arrayOf(id), null)
                            pc?.use { p -> if (p.moveToNext()) number = p.getString(0) }
                        }
                    }
                    if (number != null) {
                        return openWhatsAppDraft(number!!, text, true)
                    }
                }
                false
            } catch (e: Exception) { false }
        }

        /* ===== CAMERA / VISION ===== */
        @JavascriptInterface
        fun takePhoto(): Boolean {
            return try {
                val dir = cacheDir
                val f = java.io.File(dir, "maya_photo.jpg")
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    this@MainActivity, "$packageName.fileprovider", f)
                val i = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri)
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivityForResult(i, 5001)
                true
            } catch (e: Exception) { false }
        }

        @JavascriptInterface
        fun pickImage(): Boolean {
            return try {
                val i = Intent(Intent.ACTION_GET_CONTENT)
                i.type = "image/*"
                startActivityForResult(Intent.createChooser(i, "Photo chunko"), 5002)
                true
            } catch (e: Exception) { false }
        }

        @JavascriptInterface
        fun markAlive() { webViewAlive = true }

        /** Universal HTTP (CORS-proof) — backup brains ke liye */
        @JavascriptInterface
        fun httpPost(url: String, authHeader: String, body: String): String {
            return try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 6000
                conn.readTimeout = 15000
                conn.setRequestProperty("Content-Type", "application/json")
                if (authHeader.isNotEmpty()) conn.setRequestProperty("Authorization", authHeader)
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val txt = (if (code in 200..399) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() } ?: ""
                conn.disconnect()
                JSONObject().put("status", code).put("body", txt).toString()
            } catch (e: Exception) {
                JSONObject().put("status", 0).put("body", e.message ?: "error").toString()
            }
        }

        @JavascriptInterface
        fun httpGet(url: String, authHeader: String): String {
            return try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 8000
                if (authHeader.isNotEmpty()) conn.setRequestProperty("Authorization", authHeader)
                val code = conn.responseCode
                val txt = (if (code in 200..399) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() } ?: ""
                conn.disconnect()
                JSONObject().put("status", code).put("body", txt).toString()
            } catch (e: Exception) {
                JSONObject().put("status", 0).put("body", e.message ?: "error").toString()
            }
        }

        /** Persistent prefs (boot autostart wake) */
        @JavascriptInterface
        fun setPref(k: String, v: Boolean) { try { prefs().edit().putBoolean(k, v).apply() } catch (e: Exception) {} }

        @JavascriptInterface
        fun getPref(k: String): Boolean = try { prefs().getBoolean(k, false) } catch (e: Exception) { false }

        @JavascriptInterface
        fun getPrefString(k: String): String = try { prefs().getString(k, "") ?: "" } catch (e: Exception) { "" }

        @JavascriptInterface
        fun clearPref(k: String) { try { prefs().edit().remove(k).apply() } catch (e: Exception) {} }

        /** Auto-listen mode — screen jagti rahe */
        @JavascriptInterface
        fun keepScreenOn(on: Boolean) {
            runOnUiThread {
                if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    /* ================= HELPERS ================= */

    fun evalAsyncPublic(js: String) { evalAsync(js) }

    private fun prefs() = getSharedPreferences("maya", Context.MODE_PRIVATE)

    private fun evalAsync(js: String) {
        webView.post { webView.evaluateJavascript(js, null) }
    }

    private fun jsEscape(s: String): String = s
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\"", "\\\"")
        .replace("\n", " ")
        .replace("\r", " ")

    private fun toast(msg: String) {
        runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "MAYA Notifications", NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun requestNeededPermissions() {
        val wanted = mutableListOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            wanted.add(Manifest.permission.POST_NOTIFICATIONS)
        val need = wanted.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (need.isNotEmpty())
            ActivityCompat.requestPermissions(this, need.toTypedArray(), REQ_PERMS)
    }

    private fun requestMicPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.RECORD_AUDIO), REQ_PERMS
        )
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_PERMS
            )
    }
}
