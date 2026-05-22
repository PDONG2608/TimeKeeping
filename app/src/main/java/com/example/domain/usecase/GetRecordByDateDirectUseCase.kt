package com.example.domain.usecase

import com.example.domain.model.TimeRecord
import com.example.domain.repository.TimeRecordRepository

class GetRecordByDateDirectUseCase(private val repository: TimeRecordRepository) {
    suspend operator fun invoke(dateString: String): TimeRecord? {
        return repository.getRecordByDateDirect(dateString)
    }
}
