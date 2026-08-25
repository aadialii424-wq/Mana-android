package com.maya.ai

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

/**
 * MAYA AutoSend (Phase 5 — TABAHI LEVEL ☠️)
 * Jab Maya WhatsApp draft kholti hai (aur auto-send ON ho),
 * ye service khud SEND button dhoond kar dabati hai — bilkul hands-free.
 * 
 * Enable: Phone Settings → Accessibility → MAYA AutoSend → ON
 * Sirf com.whatsapp package watch karti hai. 45 second window ke andar.
 */
class AutoSendService : AccessibilityService() {

    companion object {
        fun pending(ctx: Context): Boolean {
            return try {
                val t = ctx.getSharedPreferences("maya", Context.MODE_PRIVATE)
                    .getLong("autosend_at", 0L)
                t > 0L && System.currentTimeMillis() - t < 45000L
            } catch (e: Exception) { false }
        }

        fun consume(ctx: Context) {
            try {
                ctx.getSharedPreferences("maya", Context.MODE_PRIVATE)
                    .edit().putLong("autosend_at", 0L).apply()
            } catch (e: Exception) {}
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.packageName != "com.whatsapp") return
        if (!pending(this)) return
        val root = rootInActiveWindow ?: return
        try {
            if (findAndClick(root)) {
                consume(this)
                handler.post {
                    Toast.makeText(this, "MAYA: message bhej diya \u2713", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {}
    }

    private fun findAndClick(root: AccessibilityNodeInfo): Boolean {
        // Layer 1: WhatsApp ke mashhoor send button IDs
        val ids = listOf(
            "com.whatsapp:id/send",
            "com.whatsapp:id/send_button",
            "com.whatsapp:id/entry_send_button"
        )
        for (id in ids) {
            try {
                root.findAccessibilityNodeInfosByViewId(id).forEach { n ->
                    if (n.isClickable && clickUp(n)) return true
                }
            } catch (e: Exception) {}
        }
        // Layer 2: "Send" text/content-desc se dhoondo
        for (label in listOf("Send", "send")) {
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
