package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompletedReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
      val prefs = ReminderPreferences(context)
      WaterReminderScheduler.syncAll(context, prefs.loadAll())
      WaterReminderScheduler.syncAllMed(context, prefs.loadAllMedReminders())
    }
  }
}
