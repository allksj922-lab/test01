package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MedicationSlot {
  MORNING,
  LUNCH,
  DINNER,
}

@Entity(tableName = "daily_health_records")
data class DailyHealthRecord(
  @PrimaryKey val dateKey: String,
  val morningMedTaken: Boolean = false,
  val lunchMedTaken: Boolean = false,
  val dinnerMedTaken: Boolean = false,
  val waterCups: Int = 0,
) {
  val takenMedicationCount: Int
    get() =
      (if (morningMedTaken) 1 else 0) +
        (if (lunchMedTaken) 1 else 0) +
        (if (dinnerMedTaken) 1 else 0)
}
