package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.MedicationSlot

class MedicationReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val slotName = intent.getStringExtra(WaterReminderScheduler.EXTRA_MED_SLOT)
    val slot = slotName?.let { runCatching { MedicationSlot.valueOf(it) }.getOrNull() }

    WaterReminderNotifier.showMedicationReminderNotification(context, slot)

    // Reschedule for tomorrow if still enabled
    if (slot != null) {
      val config = ReminderPreferences(context).loadMedSlot(slot)
      if (config.enabled) {
        WaterReminderScheduler.scheduleMedSlot(context, config)
      }
    }
  }
}
