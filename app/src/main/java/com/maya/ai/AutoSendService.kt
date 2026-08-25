package com.maya.ai

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

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

    override fun onInterrupt() {}
}
