package com.example.domain.repository

import com.example.domain.model.TimeRecord
import kotlinx.coroutines.flow.Flow

interface TimeRecordRepository {
    /**
     * Flow of all persisted time records, sorted chronologically descending.
     */
    fun getAllRecords(): Flow<List<TimeRecord>>

    /**
     * Flow of a single time record matching the specific date string (YYYY-MM-DD).
     */
    fun getRecordByDate(dateString: String): Flow<TimeRecord?>

    /**
     * Suspended query to retrieve a single time record directly outside of continuous stream.
     */
    suspend fun getRecordByDateDirect(dateString: String): TimeRecord?

    /**
     * Saves or overrides a time record.
     */
    suspend fun insertRecord(record: TimeRecord)

    /**
     * Removes a single record by key date.
     */
    suspend fun deleteRecord(dateString: String)
}
