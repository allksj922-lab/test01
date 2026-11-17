package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WaterReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val slotName = intent.getStringExtra(WaterReminderScheduler.EXTRA_SLOT_ID)
    val slotId = slotName?.let { runCatching { ReminderSlotId.valueOf(it) }.getOrNull() }

    WaterReminderNotifier.showReminderNotification(context, slotId)

    // Reschedule for tomorrow if still enabled
    if (slotId != null) {
      val config = ReminderPreferences(context).loadSlot(slotId)
      if (config.enabled) {
        WaterReminderScheduler.scheduleSlot(context, config)
      }
    }
  }
}
