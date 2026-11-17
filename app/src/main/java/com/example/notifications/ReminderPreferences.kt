package com.example.notifications

import android.content.Context
import com.example.data.MedicationSlot

enum class ReminderSlotId(val requestCode: Int, val defaultHour: Int, val defaultMinute: Int) {
  MORNING(requestCode = 1001, defaultHour = 9, defaultMinute = 30),
  AFTERNOON(requestCode = 1002, defaultHour = 14, defaultMinute = 0),
  EVENING(requestCode = 1003, defaultHour = 19, defaultMinute = 0),
}

data class WaterReminderConfig(
  val slotId: ReminderSlotId,
  val hour: Int,
  val minute: Int,
  val enabled: Boolean,
)

data class MedReminderConfig(
  val slot: MedicationSlot,
  val hour: Int,
  val minute: Int,
  val enabled: Boolean,
)

fun MedicationSlot.defaultReminderHour(): Int =
  when (this) {
    MedicationSlot.MORNING -> 8
    MedicationSlot.LUNCH -> 12
    MedicationSlot.DINNER -> 18
  }

fun MedicationSlot.defaultReminderMinute(): Int =
  when (this) {
    MedicationSlot.MORNING -> 0
    MedicationSlot.LUNCH -> 30
    MedicationSlot.DINNER -> 30
  }

fun MedicationSlot.reminderRequestCode(): Int =
  when (this) {
    MedicationSlot.MORNING -> 2001
    MedicationSlot.LUNCH -> 2002
    MedicationSlot.DINNER -> 2003
  }

class ReminderPreferences(context: Context) {
  private val prefs =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun loadAll(): List<WaterReminderConfig> = ReminderSlotId.entries.map { loadSlot(it) }

  fun loadSlot(slotId: ReminderSlotId): WaterReminderConfig {
    val hour = prefs.getInt(keyHour(slotId), slotId.defaultHour).coerceIn(0, 23)
    val minute = prefs.getInt(keyMinute(slotId), slotId.defaultMinute).coerceIn(0, 59)
    val enabled = prefs.getBoolean(keyEnabled(slotId), false)
    return WaterReminderConfig(
      slotId = slotId,
      hour = hour,
      minute = minute,
      enabled = enabled,
    )
  }

  fun saveSlot(config: WaterReminderConfig) {
    prefs
      .edit()
      .putInt(keyHour(config.slotId), config.hour.coerceIn(0, 23))
      .putInt(keyMinute(config.slotId), config.minute.coerceIn(0, 59))
      .putBoolean(keyEnabled(config.slotId), config.enabled)
      .apply()
  }

  fun loadAllMedReminders(): List<MedReminderConfig> =
    MedicationSlot.entries.map { loadMedSlot(it) }

  fun loadMedSlot(slot: MedicationSlot): MedReminderConfig {
    val hour = prefs.getInt(keyMedHour(slot), slot.defaultReminderHour()).coerceIn(0, 23)
    val minute = prefs.getInt(keyMedMinute(slot), slot.defaultReminderMinute()).coerceIn(0, 59)
    val enabled = prefs.getBoolean(keyMedEnabled(slot), false)
    return MedReminderConfig(
      slot = slot,
      hour = hour,
      minute = minute,
      enabled = enabled,
    )
  }

  fun saveMedSlot(config: MedReminderConfig) {
    prefs
      .edit()
      .putInt(keyMedHour(config.slot), config.hour.coerceIn(0, 23))
      .putInt(keyMedMinute(config.slot), config.minute.coerceIn(0, 59))
      .putBoolean(keyMedEnabled(config.slot), config.enabled)
      .apply()
  }

  private fun keyHour(slotId: ReminderSlotId) = "reminder_${slotId.name}_hour"

  private fun keyMinute(slotId: ReminderSlotId) = "reminder_${slotId.name}_minute"

  private fun keyEnabled(slotId: ReminderSlotId) = "reminder_${slotId.name}_enabled"

  private fun keyMedHour(slot: MedicationSlot) = "med_reminder_${slot.name}_hour"

  private fun keyMedMinute(slot: MedicationSlot) = "med_reminder_${slot.name}_minute"

  private fun keyMedEnabled(slot: MedicationSlot) = "med_reminder_${slot.name}_enabled"

  companion object {
    private const val PREFS_NAME = "water_reminder_prefs"
  }
}
