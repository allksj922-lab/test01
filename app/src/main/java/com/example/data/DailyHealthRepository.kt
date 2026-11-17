package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DailyHealthRepository(private val dao: DailyHealthDao) {
  private val writeMutex = Mutex()

  fun observeRecord(dateKey: String): Flow<DailyHealthRecord> {
    return dao.observeRecordForDate(dateKey).map { record ->
      record ?: DailyHealthRecord(dateKey = dateKey)
    }
  }

  fun observeRecordsInRange(
    startDateKey: String,
    endDateKey: String,
  ): Flow<List<DailyHealthRecord>> {
    return dao.observeRecordsInRange(startDateKey, endDateKey)
  }

  fun observeMonthRecords(monthPrefix: String): Flow<List<DailyHealthRecord>> {
    return dao.observeRecordsByPrefix(monthPrefix)
  }

  suspend fun toggleMedication(dateKey: String, slot: MedicationSlot) {
    writeMutex.withLock {
      val current = dao.getRecordForDate(dateKey) ?: DailyHealthRecord(dateKey = dateKey)
      val updated =
        when (slot) {
          MedicationSlot.MORNING -> current.copy(morningMedTaken = !current.morningMedTaken)
          MedicationSlot.LUNCH -> current.copy(lunchMedTaken = !current.lunchMedTaken)
          MedicationSlot.DINNER -> current.copy(dinnerMedTaken = !current.dinnerMedTaken)
        }
      dao.upsertRecord(updated)
    }
  }

  suspend fun incrementWater(dateKey: String) {
    writeMutex.withLock {
      val current = dao.getRecordForDate(dateKey) ?: DailyHealthRecord(dateKey = dateKey)
      val updated = current.copy(waterCups = current.waterCups + 1)
      dao.upsertRecord(updated)
    }
  }

  suspend fun decrementWater(dateKey: String) {
    writeMutex.withLock {
      val current = dao.getRecordForDate(dateKey) ?: DailyHealthRecord(dateKey = dateKey)
      if (current.waterCups > 0) {
        dao.upsertRecord(current.copy(waterCups = current.waterCups - 1))
      }
    }
  }

  suspend fun clearRecord(dateKey: String) {
    writeMutex.withLock { dao.deleteRecordForDate(dateKey) }
  }

  suspend fun pruneOlderRecords(oldestKeptDateKey: String) {
    writeMutex.withLock { dao.deleteRecordsBefore(oldestKeptDateKey) }
  }
}
