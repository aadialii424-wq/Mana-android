package com.maya.ai

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

/**
 * MAYA AutoSend 2.0 (Phase 9)
 * WhatsApp/Telegram draft khulte hi khud SEND dabati hai + wapas aa jati hai.
 * Enable: Phone Settings → Accessibility → MAYA AutoSend → ON
 */
class AutoSendService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: AutoSendService? = null

        @JvmStatic
        fun pending(ctx: Context): Boolean {
            return try {
                val t = ctx.getSharedPreferences("maya", Context.MODE_PRIVATE)
                    .getLong("autosend_at", 0L)
                t > 0L && System.currentTimeMillis() - t < 45000L
            } catch (e: Exception) { false }
        }

        @JvmStatic
        fun consume(ctx: Context) {
            try {
                ctx.getSharedPreferences("maya", Context.MODE_PRIVATE)
                    .edit().putLong("autosend_at", 0L).apply()
            } catch (e: Exception) {}
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName ?: return
        if (pkg != "com.whatsapp" && pkg != "com.whatsapp.w4b" && pkg != "com.telegram.messenger") return
        if (!pending(this)) return
        val root = rootInActiveWindow ?: return
        try {
            if (findAndClick(root)) {
                consume(this)
                handler.post {
                    Toast.makeText(this, "MAYA: bhej diya \u2713", Toast.LENGTH_SHORT).show()
                    try {
                        MainActivity.instance?.evalAsyncPublic("window.__autoSent && window.__autoSent()")
                    } catch (x: Exception) {}
                    handler.postDelayed({
                        try { performGlobalAction(GLOBAL_ACTION_BACK) } catch (x: Exception) {}
                    }, 1200)
                }
            }
        } catch (e: Exception) {}
    }

    private fun findAndClick(root: AccessibilityNodeInfo): Boolean {
        // Layer 1: mashhoor send button IDs
        val ids = listOf(
            "com.whatsapp:id/send",
            "com.whatsapp:id/send_button",
            "com.whatsapp:id/entry_send_button",
            "com.whatsapp.w4b:id/send",
            "com.whatsapp.w4b:id/send_button",
            "org.telegram.messenger:id/btn_send"  // Telegram (kabhi kabhi)
        )
        for (id in ids) {
            try {
                root.findAccessibilityNodeInfosByViewId(id).forEach { n ->
                    if (n.isClickable && clickUp(n)) return true
                }
            } catch (e: Exception) {}
        }
        // Layer 2: text/content-desc multi-language
        for (label in listOf("Send", "send", "Bhejo", "bhejo", "Bhej", "Enviar")) {
            try {
                root.findAccessibilityNodeInfosByText(label).forEach { n ->
                    if (clickUp(n)) return true
                }
            } catch (e: Exception) {}
        }
        return false
    }

    private fun clickUp(node: AccessibilityNodeInfo): Boolean {
        return try {
            var n: AccessibilityNodeInfo? = node
            var up = 0
            while (n != null && up < 5) {
                if (n.isClickable && n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
                n = n.parent
                up++
            }
            false
        } catch (e: Exception) { false }
    }

    /* ═══════════════════════════════════════════════════════════════════
       👁️  NAZAR  —  screen ko parh kar MATN bana dena        (P7a v5.6.0)
       -------------------------------------------------------------------
       Ye hissa screen ko CHHUTA NAHI. Sirf DEKHTA hai.

       Android har app ki screen ka ek "accessibility tree" rakhta hai —
       har button, har matn, har khana. Hum us tree par chalte hain aur
       sirf KAAM KI cheezein nikaalte hain:

         * jo nazar aa rahi ho              (isVisibleToUser)
         * jo dabai ja sake, likhi ja sake, ya scroll ho sake
         * ya jis par koi matn likha ho

       Baqi sab phenk dete hain. Wajah: Chrome ke ek page mein 500+ node
       hote hain, aur poora tree dimaag ko bhejenge to prompt phat jayega
       (CHHED 9 ka sabaq). Is liye yahin, Kotlin mein, chhaan lete hain.
       ═══════════════════════════════════════════════════════════════════ */
    fun dumpScreen(max: Int): String {
        val o = JSONObject()
        val root = try { rootInActiveWindow } catch (e: Exception) { null }
        if (root == null) {
            o.put("ok", false)
            o.put("why", "screen nahi mili \u2014 accessibility band hai ya screen locked hai")
            return o.toString()
        }
        val arr = JSONArray()
        val cap = if (max in 1..200) max else 60
        try { walk(root, arr, 0, cap) } catch (e: Exception) {}
        o.put("ok", true)
        o.put("pkg", (root.packageName ?: "").toString())
        o.put("n", arr.length())
        o.put("items", arr)
        return o.toString()
    }

    /** node ki qism — dimaag ko isi se pata chalta hai ke kya kar sakta hai */
    private fun kindOf(n: AccessibilityNodeInfo): String {
        if (n.isEditable) return "input"
        if (n.isCheckable) return "toggle"
        if (n.isClickable) return "btn"
        if (n.isScrollable) return "scroll"
        return "text"
    }

    private fun walk(n: AccessibilityNodeInfo?, out: JSONArray, depth: Int, cap: Int) {
        if (n == null || out.length() >= cap || depth > 22) return
        try {
            val txt = (n.text ?: "").toString().trim()
            val desc = (n.contentDescription ?: "").toString().trim()
            val hint = try { (n.hintText ?: "").toString().trim() } catch (e: Exception) { "" }
            var label = if (txt.isNotEmpty()) txt else if (desc.isNotEmpty()) desc else hint
            val useful = n.isClickable || n.isEditable || n.isScrollable || n.isCheckable
            var vis = false
            try { vis = n.isVisibleToUser } catch (e: Exception) { vis = true }

            if (vis && (useful || label.isNotEmpty())) {
                val r = Rect()
                n.getBoundsInScreen(r)
                if (r.width() > 4 && r.height() > 4) {
                    if (label.length > 60) label = label.substring(0, 60) + "\u2026"
                    val j = JSONObject()
                    j.put("i", out.length())
                    j.put("t", kindOf(n))
                    j.put("x", label)
                    j.put("cx", r.centerX())
                    j.put("cy", r.centerY())
                    try {
                        val vid = n.viewIdResourceName
                        if (vid != null && vid.contains('/')) j.put("id", vid.substringAfterLast('/'))
                    } catch (e: Exception) {}
                    if (n.isEditable) j.put("e", 1)
                    if (n.isScrollable) j.put("s", 1)
                    if (n.isCheckable) j.put("c", if (n.isChecked) 1 else 0)
                    out.put(j)
                }
            }
            val kids = n.childCount
            for (i in 0 until kids) {
                if (out.length() >= cap) return
                walk(n.getChild(i), out, depth + 1, cap)
            }
        } catch (e: Exception) {}
    }

    override fun onInterrupt() {}
}
