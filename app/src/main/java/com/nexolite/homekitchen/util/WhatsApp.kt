package com.nexolite.homekitchen.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object WhatsApp {

    private val packages = listOf("com.whatsapp", "com.whatsapp.w4b")

    /** Opens WhatsApp with [text] ready to send; the user picks the group. Falls back to the share sheet. */
    fun shareText(context: Context, text: String) {
        for (pkg in packages) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                setPackage(pkg)
            }
            try {
                context.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // Try the next WhatsApp package.
            }
        }
        val chooser = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            },
            "Share menu"
        )
        context.startActivity(chooser)
    }

    /** Opens a chat with one customer. Pakistani numbers like 0300 1234567 are converted to 92300... */
    fun messageNumber(context: Context, phone: String, text: String) {
        val digits = phone.filter { it.isDigit() }
        val international = when {
            digits.startsWith("92") -> digits
            digits.startsWith("0") -> "92" + digits.drop(1)
            digits.length == 10 && digits.startsWith("3") -> "92$digits"
            else -> digits
        }
        if (international.length < 10) {
            Toast.makeText(context, "Add a valid phone number for this customer first.", Toast.LENGTH_LONG).show()
            return
        }
        val uri = Uri.parse("https://wa.me/$international?text=" + Uri.encode(text))
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "WhatsApp is not installed.", Toast.LENGTH_LONG).show()
        }
    }
}
