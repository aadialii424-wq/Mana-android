package com.maya.ai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Phone restart hone par wake word khud ON ho jaye (agar ON tha) */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            try {
                if (context.getSharedPreferences("maya", Context.MODE_PRIVATE)
                        .getBoolean("wake", false)) {
                    WakeWordService.start(context)
                }
            } catch (e: Exception) {}
        }
    }
}
