
package com.prefixblock

import android.telecom.Call
import android.telecom.CallScreeningService
import android.provider.ContactsContract
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrefixCallScreeningService : CallScreeningService() {

    override fun onScreenCall(details: Call.Details) {
        if (details.callDirection != Call.Details.DIRECTION_INCOMING) {
            allowCall(details)
            return
        }

        val number = details.handle?.schemeSpecificPart.orEmpty()

        if (number.isBlank()) {
            allowCall(details)
            return
        }

        if (isInContacts(number)) {
            allowCall(details)
            return
        }

        val prefixes = getSharedPreferences(
            "settings", MODE_PRIVATE
        ).getString("prefixes", "073214")
            .orEmpty()
            .split(",")
            .map { it.trim().filter(Char::isDigit) }
            .filter { it.isNotEmpty() }

        val normalized = number.filter(Char::isDigit)
        val matchedPrefix = prefixes.firstOrNull {
            normalized.startsWith(it)
        }

        if (matchedPrefix != null) {
            saveBlockedCall(number, matchedPrefix)

            val response = CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()

            respondToCall(details, response)
        } else {
            allowCall(details)
        }
    }

    private fun allowCall(details: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .build()
        respondToCall(details, response)
    }

    private fun isInContacts(number: String): Boolean {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )

        return try {
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null,
                null,
                null
            )?.use { it.moveToFirst() } ?: false
        } catch (_: SecurityException) {
            false
        }
    }

    private fun saveBlockedCall(number: String, prefix: String) {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val timestamp = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(Date())

        val entry = "$timestamp | $number | $prefix"
        val oldLog = prefs.getString("blocked_log", "").orEmpty()
        val newLog = listOf(oldLog, entry)
            .filter { it.isNotBlank() }
            .takeLast(500)
            .joinToString("\n")

        prefs.edit().putString("blocked_log", newLog).apply()
    }
}

