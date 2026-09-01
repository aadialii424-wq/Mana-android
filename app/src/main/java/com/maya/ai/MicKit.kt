package com.maya.ai

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.MicrophoneDirection
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import org.json.JSONObject
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * ═══════════════════════════════════════════════════════════════════════════
 *  🎤 MicKit — "sab se qareebi awaaz suno, background ignore karo"   (P8c)
 * ───────────────────────────────────────────────────────────────────────────
 *  User: "Maya ko qareebi awaaz par gaur karna chahiye, background ignore kare."
 *
 *  Android mein is ke liye ASLI API maujood hai (API 29+) — aur hum ek bhi
 *  istemal nahi kar rahe the:
 *
 *    setPreferredMicrophoneFieldDimension(zoom)
 *        -1.0 = wide angle (poora kamra)  ·  0 = aam  ·  +1.0 = MAXIMUM ZOOM
 *    setPreferredMicrophoneDirection(MIC_DIRECTION_TOWARDS_USER)
 *    AudioSource.VOICE_RECOGNITION   (ASR ke liye bana source + AGC)
 *    NoiseSuppressor · AutomaticGainControl · AcousticEchoCanceler
 *
 *  ⚠️ IMAANDARI: ye DARKHWAST hain, hukm nahi. Har device support nahi karta,
 *     aur har call true/false lautati hai. Is liye hum HAR EK ka natija
 *     yaad rakhte hain aur DOCTOR mein saaf dikhate hain — andaza nahi, saboot.
 *
 *  Aur "nazdeeki ka paimana" (SNR):
 *     qareeb ki awaaz BULAND hoti hai, TV/doosra kamra DHEEMA.
 *     kamre ka shor naapo -> awaaz naapo -> farq (SNR) chhota hai to RAD.
 * ═══════════════════════════════════════════════════════════════════════════
 */
object MicKit {

    const val RATE = 16000
    private var ns: NoiseSuppressor? = null
    private var agc: AutomaticGainControl? = null
    private var aec: AcousticEchoCanceler? = null

    /** kis kis cheez ne kaam kiya — DOCTOR isay dikhata hai */
    var fxZoom = false
    var fxDir = false
    var fxNs = false
    var fxAgc = false
    var fxAec = false
    var lastErr = ""

    fun bufSize(): Int {
        val m = AudioRecord.getMinBufferSize(
            RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        return if (m > 0) m * 2 else RATE
    }

    /**
     * Mic khol kar us par ZOOM aur shor-kush lagao.
     * zoom: -1 (poora kamra) .. 0 (aam) .. +1 (sirf qareeb)
     */
    fun open(zoom: Float): AudioRecord? {
        release()
        fxZoom = false; fxDir = false; fxNs = false; fxAgc = false; fxAec = false
        lastErr = ""
        val rec = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,   /* ASR ke liye bana source */
                RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize()
            )
        } catch (e: Exception) { lastErr = e.message ?: "mic nahi khula"; return null }

        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            lastErr = "mic initialise nahi hua (shayad koi aur app istemal kar rahi hai)"
            try { rec.release() } catch (e: Exception) {}
            return null
        }

        /* 🔍 ZOOM — yehi "background ignore" wali cheez hai (API 29+) */
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                fxDir = rec.setPreferredMicrophoneDirection(MicrophoneDirection.MIC_DIRECTION_TOWARDS_USER)
            } catch (e: Exception) { fxDir = false }
            try {
                var z = zoom
                if (z < -1f) z = -1f
                if (z > 1f) z = 1f
                fxZoom = rec.setPreferredMicrophoneFieldDimension(z)
            } catch (e: Exception) { fxZoom = false }
        }

        val sid = try { rec.audioSessionId } catch (e: Exception) { -1 }
        if (sid > 0) {
            try {
                if (NoiseSuppressor.isAvailable()) {
                    ns = NoiseSuppressor.create(sid); ns?.enabled = true; fxNs = ns?.enabled == true
                }
            } catch (e: Exception) { fxNs = false }
            try {
                if (AutomaticGainControl.isAvailable()) {
                    agc = AutomaticGainControl.create(sid); agc?.enabled = true; fxAgc = agc?.enabled == true
                }
            } catch (e: Exception) { fxAgc = false }
            try {
                /* Maya apni hi awaaz par na jaage */
                if (AcousticEchoCanceler.isAvailable()) {
                    aec = AcousticEchoCanceler.create(sid); aec?.enabled = true; fxAec = aec?.enabled == true
                }
            } catch (e: Exception) { fxAec = false }
        }
        return rec
    }

    fun release() {
        try { ns?.release() } catch (e: Exception) {}
        try { agc?.release() } catch (e: Exception) {}
        try { aec?.release() } catch (e: Exception) {}
        ns = null; agc = null; aec = null
    }

    /** ek buffer ki shiddat, dB mein (taqreeban 0..90) */
    fun db(buf: ShortArray, n: Int): Double {
        if (n <= 0) return 0.0
        var sum = 0.0
        for (i in 0 until n) { val v = buf[i].toDouble(); sum += v * v }
        val rms = sqrt(sum / n)
        if (rms < 1.0) return 0.0
        return 20.0 * log10(rms) + 10.0      /* motay tor par dB-ish paimana */
    }

    fun fxJson(o: JSONObject): JSONObject {
        o.put("zoom", fxZoom); o.put("dir", fxDir)
        o.put("ns", fxNs); o.put("agc", fxAgc); o.put("aec", fxAec)
        if (lastErr.isNotEmpty()) o.put("err", lastErr)
        return o
    }

    /**
     * 🧪 MIC TEST — kamre ka shor, aap ki awaaz, aur farq (SNR).
     * Pehla 1 second = khamoshi ka farsh. Baqi = aap boliye.
     */
    fun test(ms: Int, zoom: Float): String {
        val o = JSONObject()
        val rec = open(zoom)
        if (rec == null) {
            o.put("ok", false); o.put("why", lastErr.ifEmpty { "mic nahi khula" })
            return fxJson(o).toString()
        }
        val buf = ShortArray(1600)               /* 100ms */
        var floor = 99.0
        var peak = 0.0
        var voiced = 0
        var frames = 0
        try {
            rec.startRecording()
            val until = System.currentTimeMillis() + (if (ms in 1000..15000) ms else 5000)
            val quietUntil = System.currentTimeMillis() + 1000
            while (System.currentTimeMillis() < until) {
                val n = rec.read(buf, 0, buf.size)
                if (n <= 0) continue
                val d = db(buf, n)
                frames++
                if (System.currentTimeMillis() < quietUntil) {
                    if (d < floor) floor = d
                } else {
                    if (d > peak) peak = d
                    if (d > floor + 12.0) voiced++
                }
            }
            rec.stop()
        } catch (e: Exception) {
            o.put("ok", false); o.put("why", e.message ?: "test nakaam")
            try { rec.release() } catch (x: Exception) {}
            release()
            return fxJson(o).toString()
        }
        try { rec.release() } catch (e: Exception) {}
        release()
        if (floor > 90.0) floor = 0.0
        o.put("ok", true)
        o.put("floor", Math.round(floor))
        o.put("peak", Math.round(peak))
        o.put("snr", Math.round(peak - floor))
        o.put("voiced", voiced)
        o.put("frames", frames)
        return fxJson(o).toString()
    }
}
