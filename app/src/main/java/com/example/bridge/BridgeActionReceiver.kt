package com.example.bridge

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

class BridgeActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_COPY_OTP -> {
                val otp = intent.getStringExtra(EXTRA_OTP).orEmpty()
                if (otp.isNotBlank()) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("SimLink OTP", otp))
                    val msg = if (BridgeSessionHub.languageCode == "en") {
                        "OTP copied: $otp"
                    } else {
                        "کد تایید کپی شد: $otp"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
            ACTION_TOGGLE_VOICE -> {
                BridgeSessionHub.toggleVoiceFromSystem(context)
            }
            ACTION_OPEN_PHONE -> {
                ITellTelecomManager.openDefaultPhoneApp(context)
            }
            ACTION_OPEN_MESSAGES -> {
                ITellTelecomManager.openDefaultMessagesApp(context)
            }
        }
    }

    companion object {
        const val ACTION_COPY_OTP = "com.example.itell.ACTION_COPY_OTP"
        const val ACTION_TOGGLE_VOICE = "com.example.itell.ACTION_TOGGLE_VOICE"
        const val ACTION_OPEN_PHONE = "com.example.itell.ACTION_OPEN_PHONE"
        const val ACTION_OPEN_MESSAGES = "com.example.itell.ACTION_OPEN_MESSAGES"
        const val EXTRA_OTP = "extra_otp"
    }
}
