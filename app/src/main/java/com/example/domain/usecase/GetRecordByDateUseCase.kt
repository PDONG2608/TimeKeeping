package com.example.domain.usecase

import com.example.domain.model.TimeRecord
import com.example.domain.repository.TimeRecordRepository
import kotlinx.coroutines.flow.Flow

class GetRecordByDateUseCase(private val repository: TimeRecordRepository) {
    operator fun invoke(dateString: String): Flow<TimeRecord?> {
        return repository.getRecordByDate(dateString)
    }
}
