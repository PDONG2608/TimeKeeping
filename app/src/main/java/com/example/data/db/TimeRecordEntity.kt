package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.TimeRecord

@Entity(tableName = "time_records")
data class TimeRecordEntity(
    @PrimaryKey val dateString: String, // format "YYYY-MM-DD"
    val checkInTime: String? = null,    // format "HH:mm"
    val checkInTimestamp: Long? = null, // unix timestamp for live calculation
    val checkOutTime: String? = null,   // format "HH:mm"
    val requiredHours: Double = 8.0,    // number of hours needed
    val actualMinutesWorked: Int = 0,   // total actual elapsed minutes
    val isTracking: Boolean = false,    // true if timer is actively ticking
    val notes: String = ""              // daily notes
) {
    fun toDomain(): TimeRecord {
        return TimeRecord(
            dateString = dateString,
            checkInTime = checkInTime,
            checkInTimestamp = checkInTimestamp,
            checkOutTime = checkOutTime,
            requiredHours = requiredHours,
            actualMinutesWorked = actualMinutesWorked,
            isTracking = isTracking,
            notes = notes
        )
    }

    companion object {
        fun fromDomain(domain: TimeRecord): TimeRecordEntity {
            return TimeRecordEntity(
                dateString = domain.dateString,
                checkInTime = domain.checkInTime,
                checkInTimestamp = domain.checkInTimestamp,
                checkOutTime = domain.checkOutTime,
                requiredHours = domain.requiredHours,
                actualMinutesWorked = domain.actualMinutesWorked,
                isTracking = domain.isTracking,
                notes = domain.notes
            )
        }
    }
}
