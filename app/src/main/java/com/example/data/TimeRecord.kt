package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_records")
data class TimeRecord(
    @PrimaryKey val dateString: String, // format "YYYY-MM-DD"
    val checkInTime: String? = null,    // format "HH:mm"
    val checkInTimestamp: Long? = null, // unix timestamp for live calculation
    val checkOutTime: String? = null,   // format "HH:mm"
    val requiredHours: Double = 8.0,    // number of hours needed
    val actualMinutesWorked: Int = 0,   // total actual elapsed minutes
    val isTracking: Boolean = false,    // true if timer is actively ticking
    val notes: String = ""              // daily notes
)
