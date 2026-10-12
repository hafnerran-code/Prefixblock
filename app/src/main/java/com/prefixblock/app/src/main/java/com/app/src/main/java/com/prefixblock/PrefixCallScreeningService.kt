
package com.prefixblock

import android.telecom.Call
import android.telecom.CallScreeningService
import android.provider.ContactsContract
import android.net.Uri

class PrefixCallScreeningService : CallScreeningService() {

    override fun onScreenCall(details: Call.Details) {
        if (details.callDirection != Call.Details.DIRECTION_INCOMING) {
            return
        }

        val handle = details.handle
        val number = handle?.schemeSpecificPart ?: ""

        if (number.isBlank()) {
            allowCall()
            return
        }

        // Never block a number saved in contacts.
        if (isInContacts(number)) {
            allowCall()
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

        val shouldBlock = prefixes.any { prefix ->
            normalized.startsWith(prefix)
        }

        if (shouldBlock) {
            val response = CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()

            respondToCall(details, response)
        } else {
            allowCall()
        }
    }

    private fun allowCall() {
        // An empty response allows the call to continue.
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
}
