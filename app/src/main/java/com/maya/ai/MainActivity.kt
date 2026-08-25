package com.maya.ai

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
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
    }

    private lateinit var webView: WebView
    private lateinit var assetLoader: WebViewAssetLoader
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var recognizer: SpeechRecognizer? = null

    /* ================= LIFECYCLE ================= */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this)
        webView.setBackgroundColor(0xFF050B14.toInt())
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
        }
        webView.addJavascriptInterface(MayaBridge(), "MayaBridge")
        webView.webViewClient = MayaWebViewClient()
        setContentView(webView)
        webView.loadUrl("https://$VIRTUAL_HOST/assets/web/index.html")

        initTts()
        createNotificationChannel()
        requestNeededPermissions()
    }

    override fun onDestroy() {
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
                toast("Ye app/link is phone par nahi mila")
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
                }
                recognizer = SpeechRecognizer.createSpeechRecognizer(this@MainActivity).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {}
                        override fun onError(error: Int) {
                            evalAsync("window.__nativeSpeechErr && window.__nativeSpeechErr($error)")
                        }
                        override fun onResults(results: Bundle?) {
                            val text = results
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull() ?: ""
                            evalAsync("window.__nativeSpeech && window.__nativeSpeech('" + jsEscape(text) + "')")
                        }
                        override fun onPartialResults(partialResults: Bundle?) {}
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

        /** REAL system alarm — phone ke clock app mein set hota hai */
        @JavascriptInterface
        fun setAlarm(hour: Int, minute: Int, message: String): Boolean {
            return try {
                val i = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_MESSAGE, message.ifEmpty { "MAYA Alarm" })
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                }
                startActivity(i)
                true
            } catch (e: Exception) { false }
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
        val wanted = mutableListOf(Manifest.permission.RECORD_AUDIO)
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
