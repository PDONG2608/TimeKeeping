package com.example.util

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    
    fun getCurrentDateString(timezoneId: String = "Asia/Ho_Chi_Minh"): String {
        val dateFullFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dateFullFormatter.timeZone = TimeZone.getTimeZone(timezoneId)
        return dateFullFormatter.format(Date())
    }

    fun getFriendlyDate(dateString: String, timezoneId: String = "Asia/Ho_Chi_Minh"): String {
        return try {
            val dateFullFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateFullFormatter.timeZone = TimeZone.getTimeZone(timezoneId)
            val date = dateFullFormatter.parse(dateString) ?: Date()
            
            val friendlyDateFormatter = SimpleDateFormat("EEEE, d 'Tháng' M, yyyy", Locale("vi", "VN"))
            friendlyDateFormatter.timeZone = TimeZone.getTimeZone(timezoneId)
            friendlyDateFormatter.format(date)
        } catch (e: Exception) {
            dateString
        }
    }

    fun formatShortMonthDay(dateString: String, timezoneId: String = "Asia/Ho_Chi_Minh"): String {
        return try {
            val dateFullFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateFullFormatter.timeZone = TimeZone.getTimeZone(timezoneId)
            val date = dateFullFormatter.parse(dateString) ?: Date()
            
            val shortMonthDayFormatter = SimpleDateFormat("dd/MM", Locale.getDefault())
            shortMonthDayFormatter.timeZone = TimeZone.getTimeZone(timezoneId)
            shortMonthDayFormatter.format(date)
        } catch (e: Exception) {
            dateString
        }
    }

    fun getCurrentTimeFormatted(timezoneId: String = "Asia/Ho_Chi_Minh"): String {
        val parser = SimpleDateFormat("HH:mm", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone(timezoneId)
        return parser.format(Date())
    }

    fun parseHHmmToMinutes(time: String): Int? {
        val parts = time.split(":")
        if (parts.size != 2) return null
        val hours = parts[0].toIntOrNull() ?: return null
        val minutes = parts[1].toIntOrNull() ?: return null
        return hours * 60 + minutes
    }

    fun formatMinutesToHHmm(minutes: Int): String {
        val h = (minutes / 60) % 24
        val m = minutes % 60
        return String.format(Locale.getDefault(), "%02d:%02d", h, m)
    }

    fun calculateCheckOutTime(checkInTime: String, requiredHours: Double): String {
        val startMins = parseHHmmToMinutes(checkInTime) ?: return "--:--"
        val durationMins = (requiredHours * 60).toInt()
        val endMins = startMins + durationMins
        return formatMinutesToHHmm(endMins)
    }

    // Generate a list of days in the current month surrounding today
    fun getCalendarDaysList(timezoneId: String = "Asia/Ho_Chi_Minh"): List<String> {
        val dateFullFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val tz = TimeZone.getTimeZone(timezoneId)
        dateFullFormatter.timeZone = tz
        
        val daysList = mutableListOf<String>()
        val calendar = Calendar.getInstance(tz)
        
        // Go back 15 days and forward 15 days to give a robust historical and future view
        calendar.add(Calendar.DAY_OF_YEAR, -15)
        
        for (i in 0..40) {
            daysList.add(dateFullFormatter.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return daysList
    }
}
