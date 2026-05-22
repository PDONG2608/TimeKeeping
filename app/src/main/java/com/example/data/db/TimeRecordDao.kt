package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeRecordDao {
    @Query("SELECT * FROM time_records ORDER BY dateString DESC")
    fun getAllRecords(): Flow<List<TimeRecordEntity>>

    @Query("SELECT * FROM time_records WHERE dateString = :dateString")
    fun getRecordByDate(dateString: String): Flow<TimeRecordEntity?>

    @Query("SELECT * FROM time_records WHERE dateString = :dateString LIMIT 1")
    suspend fun getRecordByDateDirect(dateString: String): TimeRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: TimeRecordEntity)

    @Query("DELETE FROM time_records WHERE dateString = :dateString")
    suspend fun deleteRecordByDate(dateString: String)
}
