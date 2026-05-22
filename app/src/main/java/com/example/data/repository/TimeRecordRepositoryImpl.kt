package com.example.data.repository

import com.example.data.db.TimeRecordDao
import com.example.data.db.TimeRecordEntity
import com.example.domain.model.TimeRecord
import com.example.domain.repository.TimeRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TimeRecordRepositoryImpl(
    private val timeRecordDao: TimeRecordDao
) : TimeRecordRepository {

    override fun getAllRecords(): Flow<List<TimeRecord>> {
        return timeRecordDao.getAllRecords().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecordByDate(dateString: String): Flow<TimeRecord?> {
        return timeRecordDao.getRecordByDate(dateString).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun getRecordByDateDirect(dateString: String): TimeRecord? {
        return timeRecordDao.getRecordByDateDirect(dateString)?.toDomain()
    }

    override suspend fun insertRecord(record: TimeRecord) {
        timeRecordDao.insertRecord(TimeRecordEntity.fromDomain(record))
    }

    override suspend fun deleteRecord(dateString: String) {
        timeRecordDao.deleteRecordByDate(dateString)
    }
}
