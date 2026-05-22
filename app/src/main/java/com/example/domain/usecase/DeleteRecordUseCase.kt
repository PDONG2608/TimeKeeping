package com.example.domain.usecase

import com.example.domain.repository.TimeRecordRepository

class DeleteRecordUseCase(private val repository: TimeRecordRepository) {
    suspend operator fun invoke(dateString: String) {
        repository.deleteRecord(dateString)
    }
}
