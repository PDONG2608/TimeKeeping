package com.example.domain.model

data class TimeRecord(
    val dateString: String,
    val checkInTime: String? = null,
    val checkInTimestamp: Long? = null,
    val checkOutTime: String? = null,
    val requiredHours: Double = 8.0,
    val actualMinutesWorked: Int = 0,
    val isTracking: Boolean = false,
    val notes: String = ""
)
