package com.example.domain.usecase

data class TimeRecordUseCases(
    val getAllRecords: GetAllRecordsUseCase,
    val getRecordByDate: GetRecordByDateUseCase,
    val getRecordByDateDirect: GetRecordByDateDirectUseCase,
    val saveRecord: SaveRecordUseCase,
    val deleteRecord: DeleteRecordUseCase
)
