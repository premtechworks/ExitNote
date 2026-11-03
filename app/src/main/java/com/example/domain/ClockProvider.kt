package com.example.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

interface ClockProvider {
    fun nowMillis(): Long
    fun currentZoneId(): String
}

class SystemClockProvider : ClockProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun currentZoneId(): String = TimeZone.getDefault().id
}

object TimeFormatter {
    private fun resolveTimeZone(timezoneId: String?): TimeZone {
        if (timezoneId.isNullOrBlank()) return TimeZone.getDefault()
        val tz = TimeZone.getTimeZone(timezoneId)
        // TimeZone.getTimeZone returns GMT if unrecognized unless the ID itself was GMT/UTC
        return if (tz.id == "GMT" && !timezoneId.equals("GMT", ignoreCase = true) && !timezoneId.equals("UTC", ignoreCase = true)) {
            TimeZone.getDefault()
        } else {
            tz
        }
    }

    fun formatTime(epochMillis: Long, timezoneId: String?, locale: Locale = Locale.US): String {
        return try {
            val formatter = SimpleDateFormat("h:mm a", locale).apply {
                timeZone = resolveTimeZone(timezoneId)
            }
            formatter.format(Date(epochMillis))
        } catch (e: Exception) {
            "—"
        }
    }

    fun formatTimeWithSeconds(epochMillis: Long, timezoneId: String?, locale: Locale = Locale.US): String {
        return try {
            val formatter = SimpleDateFormat("h:mm:ss a", locale).apply {
                timeZone = resolveTimeZone(timezoneId)
            }
            formatter.format(Date(epochMillis))
        } catch (e: Exception) {
            formatTime(epochMillis, timezoneId, locale)
        }
    }

    fun formatDate(epochMillis: Long, timezoneId: String?, locale: Locale = Locale.US): String {
        return try {
            val formatter = SimpleDateFormat("EEE, MMM d, yyyy", locale).apply {
                timeZone = resolveTimeZone(timezoneId)
            }
            formatter.format(Date(epochMillis))
        } catch (e: Exception) {
            "—"
        }
    }

    fun formatShortDate(epochMillis: Long, timezoneId: String?, locale: Locale = Locale.US): String {
        return try {
            val formatter = SimpleDateFormat("MMM d, yyyy", locale).apply {
                timeZone = resolveTimeZone(timezoneId)
            }
            formatter.format(Date(epochMillis))
        } catch (e: Exception) {
            "—"
        }
    }
}
