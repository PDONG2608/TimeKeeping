package com.example.domain.usecase

import com.example.domain.model.TimeRecord
import com.example.domain.repository.TimeRecordRepository

class SaveRecordUseCase(private val repository: TimeRecordRepository) {
    suspend operator fun invoke(record: TimeRecord) {
        repository.insertRecord(record)
    }
}
