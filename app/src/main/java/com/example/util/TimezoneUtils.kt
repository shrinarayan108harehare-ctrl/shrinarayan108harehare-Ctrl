package com.example.util

import com.example.model.DateRangePreset
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimezoneUtils {
    val IST_ZONE: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    fun todayDateKey(): String = formatDateKey(Date())

    fun formatDateKey(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = IST_ZONE
        return sdf.format(date)
    }

    fun formatDateKey(timestamp: Timestamp?): String {
        return if (timestamp == null) todayDateKey() else formatDateKey(timestamp.toDate())
    }

    fun dateKeyFromTimestamp(timestamp: Timestamp?): String = formatDateKey(timestamp)

    fun formatTimeDisplay(date: Date = Date()): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.US)
        sdf.timeZone = IST_ZONE
        return sdf.format(date)
    }

    fun formatTimeDisplay(timestamp: Timestamp?): String {
        return if (timestamp == null) "" else formatTimeDisplay(timestamp.toDate())
    }

    fun formatDateTimeDisplay(timestamp: Timestamp?): String {
        if (timestamp == null) return "Just now"
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
        sdf.timeZone = IST_ZONE
        return "${sdf.format(timestamp.toDate())} IST"
    }

    fun formatChatTime(timestamp: Timestamp?): String {
        if (timestamp == null) return ""
        val sdf = SimpleDateFormat("hh:mm a", Locale.US)
        sdf.timeZone = IST_ZONE
        return sdf.format(timestamp.toDate())
    }

    fun generateOrderNumber(): String {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
        sdf.timeZone = IST_ZONE
        val datePart = sdf.format(Date())
        val suffix = (1000..9999).random()
        return "PDS-$datePart-$suffix"
    }

    fun rangeForPreset(
        preset: DateRangePreset,
        customStartKey: String = todayDateKey(),
        customEndKey: String = todayDateKey()
    ): Pair<String, String> {
        val cal = Calendar.getInstance(IST_ZONE)
        val today = formatDateKey(cal.time)
        return when (preset) {
            DateRangePreset.TODAY -> today to today
            DateRangePreset.YESTERDAY -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val y = formatDateKey(cal.time)
                y to y
            }
            DateRangePreset.LAST_7_DAYS -> {
                cal.add(Calendar.DAY_OF_YEAR, -6)
                formatDateKey(cal.time) to today
            }
            DateRangePreset.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                formatDateKey(cal.time) to today
            }
            DateRangePreset.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                formatDateKey(cal.time) to today
            }
            DateRangePreset.LAST_MONTH -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = formatDateKey(cal.time)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = formatDateKey(cal.time)
                start to end
            }
            DateRangePreset.THIS_QUARTER -> {
                val currentMonth = cal.get(Calendar.MONTH)
                val qStartMonth = (currentMonth / 3) * 3
                cal.set(Calendar.MONTH, qStartMonth)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                formatDateKey(cal.time) to today
            }
            DateRangePreset.THIS_YEAR -> {
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                formatDateKey(cal.time) to today
            }
            DateRangePreset.CUSTOM -> {
                val s = if (customStartKey <= customEndKey) customStartKey else customEndKey
                val e = if (customStartKey <= customEndKey) customEndKey else customStartKey
                s to e
            }
        }
    }
}
