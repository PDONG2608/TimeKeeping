package com.example.domain.usecase

import com.example.domain.model.TimeRecord
import com.example.domain.repository.TimeRecordRepository
import kotlinx.coroutines.flow.Flow

class GetAllRecordsUseCase(private val repository: TimeRecordRepository) {
    operator fun invoke(): Flow<List<TimeRecord>> {
        return repository.getAllRecords()
    }
}
