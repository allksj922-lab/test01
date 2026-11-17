package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyHealthDao {
  @Query("SELECT * FROM daily_health_records WHERE dateKey = :dateKey LIMIT 1")
  fun observeRecordForDate(dateKey: String): Flow<DailyHealthRecord?>

  @Query(
    "SELECT * FROM daily_health_records WHERE dateKey >= :startDateKey AND dateKey <= :endDateKey ORDER BY dateKey ASC"
  )
  fun observeRecordsInRange(startDateKey: String, endDateKey: String): Flow<List<DailyHealthRecord>>

  @Query("SELECT * FROM daily_health_records WHERE dateKey LIKE :monthPrefix || '%' ORDER BY dateKey ASC")
  fun observeRecordsByPrefix(monthPrefix: String): Flow<List<DailyHealthRecord>>

  @Query("SELECT * FROM daily_health_records WHERE dateKey = :dateKey LIMIT 1")
  suspend fun getRecordForDate(dateKey: String): DailyHealthRecord?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRecord(record: DailyHealthRecord)

  @Query("DELETE FROM daily_health_records WHERE dateKey = :dateKey")
  suspend fun deleteRecordForDate(dateKey: String)

  @Query("DELETE FROM daily_health_records WHERE dateKey < :oldestKeptDateKey")
  suspend fun deleteRecordsBefore(oldestKeptDateKey: String)
}
