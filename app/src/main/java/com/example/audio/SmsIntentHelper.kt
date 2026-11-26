package com.example.audio

import android.content.Context
import android.content.Intent
import android.net.Uri

object SmsIntentHelper {
    fun launchDeviceSmsComposer(
        context: Context,
        phoneNumber: String,
        messageBody: String
    ): Boolean {
        val cleanPhone = phoneNumber.replace(" ", "")
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$cleanPhone")
            putExtra("sms_body", messageBody)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
