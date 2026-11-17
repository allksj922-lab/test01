package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.MedicationSlot
import java.util.Calendar

object WaterReminderScheduler {
  const val EXTRA_SLOT_ID = "extra_slot_id"
  const val EXTRA_MED_SLOT = "extra_med_slot"

  fun syncSlotSchedule(context: Context, config: WaterReminderConfig) {
    if (config.enabled) {
      scheduleSlot(context, config)
    } else {
      cancelSlot(context, config.slotId)
    }
  }

  fun syncAll(context: Context, configs: List<WaterReminderConfig>) {
    configs.forEach { syncSlotSchedule(context, it) }
  }

  fun scheduleSlot(
    context: Context,
    config: WaterReminderConfig,
    nowMillis: Long = System.currentTimeMillis(),
  ) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = buildPendingIntent(context, config.slotId)
    val triggerAtMillis = computeNextTriggerMillis(config.hour, config.minute, nowMillis)

    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
  }

  fun cancelSlot(context: Context, slotId: ReminderSlotId) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = buildPendingIntent(context, slotId)
    alarmManager.cancel(pendingIntent)
  }

  fun syncMedSlotSchedule(context: Context, config: MedReminderConfig) {
    if (config.enabled) {
      scheduleMedSlot(context, config)
    } else {
      cancelMedSlot(context, config.slot)
    }
  }

  fun syncAllMed(context: Context, configs: List<MedReminderConfig>) {
    configs.forEach { syncMedSlotSchedule(context, it) }
  }

  fun scheduleMedSlot(
    context: Context,
    config: MedReminderConfig,
    nowMillis: Long = System.currentTimeMillis(),
  ) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = buildMedPendingIntent(context, config.slot)
    val triggerAtMillis = computeNextTriggerMillis(config.hour, config.minute, nowMillis)

    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
  }

  fun cancelMedSlot(context: Context, slot: MedicationSlot) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = buildMedPendingIntent(context, slot)
    alarmManager.cancel(pendingIntent)
  }

  fun computeNextTriggerMillis(hour: Int, minute: Int, nowMillis: Long): Long {
    val target =
      Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        set(Calendar.MINUTE, minute.coerceIn(0, 59))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
    if (target.timeInMillis <= nowMillis) {
      target.add(Calendar.DAY_OF_YEAR, 1)
    }
    return target.timeInMillis
  }

  private fun buildPendingIntent(context: Context, slotId: ReminderSlotId): PendingIntent {
    val intent =
      Intent(context, WaterReminderReceiver::class.java).apply {
        putExtra(EXTRA_SLOT_ID, slotId.name)
      }
    return PendingIntent.getBroadcast(
      context,
      slotId.requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun buildMedPendingIntent(context: Context, slot: MedicationSlot): PendingIntent {
    val intent =
      Intent(context, MedicationReminderReceiver::class.java).apply {
        putExtra(EXTRA_MED_SLOT, slot.name)
      }
    return PendingIntent.getBroadcast(
      context,
      slot.reminderRequestCode(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }
}
