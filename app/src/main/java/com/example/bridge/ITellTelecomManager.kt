package com.example.bridge

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import com.example.R

object ITellTelecomManager {

    private const val ACCOUNT_ID_BRIDGE = "itell_bridge_phone_account_v1"

    fun accountHandle(context: Context): PhoneAccountHandle {
        return PhoneAccountHandle(
            ComponentName(context.applicationContext, ITellConnectionService::class.java),
            ACCOUNT_ID_BRIDGE
        )
    }

    fun registerCallingAccount(context: Context, label: String = "SimLink SIM Bridge"): Boolean {
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return false
        val handle = accountHandle(context)
        return runCatching {
            val builder = PhoneAccount.builder(handle, label)
                .setAddress(Uri.parse("tel:0000"))
                .setShortDescription("Android-to-Android SIM Bridge")
                .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
                .setIcon(Icon.createWithResource(context, R.drawable.ic_signal_bars_4))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setCapabilities(
                    PhoneAccount.CAPABILITY_CALL_PROVIDER or PhoneAccount.CAPABILITY_SELF_MANAGED
                )
            } else {
                builder.setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
            }

            // Fallback to SELF_MANAGED if OEM restricts CALL_PROVIDER without extra setup
            runCatching {
                telecom.registerPhoneAccount(builder.build())
            }.recoverCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val selfManaged = PhoneAccount.builder(handle, label)
                        .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
                        .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED)
                        .build()
                    telecom.registerPhoneAccount(selfManaged)
                } else {
                    throw it
                }
            }.isSuccess
        }.getOrDefault(false)
    }

    fun isCallingAccountEnabled(context: Context): Boolean {
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return false
        return runCatching {
            telecom.getPhoneAccount(accountHandle(context))?.isEnabled == true
        }.getOrDefault(false)
    }

    fun openCallingAccountSettings(context: Context) {
        registerCallingAccount(context)
        val intent = Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun notifyIncomingHostCall(context: Context, phoneNumberHint: String, simSlot: Int) {
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return
        runCatching {
            val extras = Bundle().apply {
                putInt("sim_slot", simSlot)
                putString("caller_hint", phoneNumberHint)
            }
            telecom.addNewIncomingCall(accountHandle(context), extras)
        }
    }

    fun openDefaultPhoneApp(context: Context) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openDefaultMessagesApp(context: Context) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MESSAGING)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            val fallback = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(fallback) }
        }
    }
}
