package com.example.notifications

import android.content.Context
import com.example.data.MedicationSlot

interface WaterReminderGateway {
  fun loadReminders(): List<WaterReminderConfig>

  fun updateReminder(config: WaterReminderConfig)

  fun sendTestNotification()

  fun loadMedReminders(): List<MedReminderConfig>

  fun updateMedReminder(config: MedReminderConfig)

  fun sendTestMedNotification()
}

class AndroidWaterReminderGateway(private val context: Context) : WaterReminderGateway {
  private val appContext = context.applicationContext
  private val prefs = ReminderPreferences(appContext)

  override fun loadReminders(): List<WaterReminderConfig> {
    val configs = prefs.loadAll()
    WaterReminderScheduler.syncAll(appContext, configs)
    return configs
  }

  override fun updateReminder(config: WaterReminderConfig) {
    prefs.saveSlot(config)
    WaterReminderNotifier.ensureNotificationChannel(appContext)
    WaterReminderScheduler.syncSlotSchedule(appContext, config)
  }

  override fun sendTestNotification() {
    WaterReminderNotifier.showReminderNotification(appContext, null)
  }

  override fun loadMedReminders(): List<MedReminderConfig> {
    val configs = prefs.loadAllMedReminders()
    WaterReminderScheduler.syncAllMed(appContext, configs)
    return configs
  }

  override fun updateMedReminder(config: MedReminderConfig) {
    prefs.saveMedSlot(config)
    WaterReminderNotifier.ensureNotificationChannel(appContext)
    WaterReminderScheduler.syncMedSlotSchedule(appContext, config)
  }

  override fun sendTestMedNotification() {
    WaterReminderNotifier.showMedicationReminderNotification(appContext, MedicationSlot.MORNING)
  }
}

object NoOpWaterReminderGateway : WaterReminderGateway {
  override fun loadReminders(): List<WaterReminderConfig> =
    ReminderSlotId.entries.map {
      WaterReminderConfig(
        slotId = it,
        hour = it.defaultHour,
        minute = it.defaultMinute,
        enabled = false,
      )
    }

  override fun updateReminder(config: WaterReminderConfig) = Unit

  override fun sendTestNotification() = Unit

  override fun loadMedReminders(): List<MedReminderConfig> =
    MedicationSlot.entries.map {
      MedReminderConfig(
        slot = it,
        hour = it.defaultReminderHour(),
        minute = it.defaultReminderMinute(),
        enabled = false,
      )
    }

  override fun updateMedReminder(config: MedReminderConfig) = Unit

  override fun sendTestMedNotification() = Unit
}
