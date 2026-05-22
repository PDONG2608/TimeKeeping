package com.example.data

import kotlinx.coroutines.flow.Flow

class TimeRecordRepository(private val timeRecordDao: TimeRecordDao) {
    val allRecords: Flow<List<TimeRecord>> = timeRecordDao.getAllRecords()

    fun getRecordByDate(dateString: String): Flow<TimeRecord?> {
        return timeRecordDao.getRecordByDate(dateString)
    }

    suspend fun getRecordByDateDirect(dateString: String): TimeRecord? {
        return timeRecordDao.getRecordByDateDirect(dateString)
    }

    suspend fun insertRecord(record: TimeRecord) {
        timeRecordDao.insertRecord(record)
    }

    suspend fun deleteRecord(dateString: String) {
        timeRecordDao.deleteRecordByDate(dateString)
    }
}
