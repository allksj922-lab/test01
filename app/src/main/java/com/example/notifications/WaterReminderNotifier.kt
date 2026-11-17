package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.MedicationSlot

object WaterReminderNotifier {
  const val CHANNEL_ID = "water_reminder_channel_v1"
  const val MED_CHANNEL_ID = "med_reminder_channel_v1"
  private const val NOTIFICATION_ID_BASE = 2000
  private const val MED_NOTIFICATION_ID_BASE = 3000
  private const val OPEN_APP_REQUEST_CODE = 3001
  private const val OPEN_APP_MED_REQUEST_CODE = 3002
  val VIBRATION_PATTERN = longArrayOf(0L, 450L, 200L, 450L)

  fun ensureNotificationChannel(context: Context) {
    ensureChannel(
      context = context,
      channelId = CHANNEL_ID,
      name = context.getString(R.string.notification_channel_name),
      descriptionText = context.getString(R.string.notification_channel_description),
    )
    ensureChannel(
      context = context,
      channelId = MED_CHANNEL_ID,
      name = context.getString(R.string.med_notification_channel_name),
      descriptionText = context.getString(R.string.med_notification_channel_description),
    )
  }

  private fun ensureChannel(
    context: Context,
    channelId: String,
    name: String,
    descriptionText: String,
  ) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    val audioAttributes =
      AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    val channel =
      NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_HIGH).apply {
        description = descriptionText
        enableVibration(true)
        vibrationPattern = VIBRATION_PATTERN
        enableLights(true)
        setSound(soundUri, audioAttributes)
      }

    manager.createNotificationChannel(channel)
  }

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun showReminderNotification(context: Context, slotId: ReminderSlotId? = null) {
    ensureNotificationChannel(context)
    if (!hasNotificationPermission(context)) return

    postNotification(
      context = context,
      channelId = CHANNEL_ID,
      smallIconRes = R.drawable.ic_water_notification,
      title = context.getString(R.string.notification_title),
      body = context.getString(R.string.notification_body),
      notificationId = NOTIFICATION_ID_BASE + (slotId?.ordinal ?: 99),
      requestCode = OPEN_APP_REQUEST_CODE,
    )
  }

  fun showMedicationReminderNotification(context: Context, slot: MedicationSlot? = null) {
    ensureNotificationChannel(context)
    if (!hasNotificationPermission(context)) return

    val title =
      when (slot) {
        MedicationSlot.MORNING, null -> context.getString(R.string.med_notification_title_morning)
        MedicationSlot.LUNCH -> context.getString(R.string.med_notification_title_lunch)
        MedicationSlot.DINNER -> context.getString(R.string.med_notification_title_dinner)
      }

    postNotification(
      context = context,
      channelId = MED_CHANNEL_ID,
      smallIconRes = R.drawable.ic_med_notification,
      title = title,
      body = context.getString(R.string.med_notification_body),
      notificationId = MED_NOTIFICATION_ID_BASE + (slot?.ordinal ?: 99),
      requestCode = OPEN_APP_MED_REQUEST_CODE,
    )
  }

  private fun postNotification(
    context: Context,
    channelId: String,
    smallIconRes: Int,
    title: String,
    body: String,
    notificationId: Int,
    requestCode: Int,
  ) {
    val launchIntent =
      Intent(context, MainActivity::class.java).apply {
        flags =
          Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
      }

    val contentPendingIntent =
      PendingIntent.getActivity(
        context,
        requestCode,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    val notification =
      NotificationCompat.Builder(context, channelId)
        .setSmallIcon(smallIconRes)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setSound(soundUri)
        .setVibrate(VIBRATION_PATTERN)
        .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
        .setAutoCancel(true)
        .setContentIntent(contentPendingIntent)
        .build()

    try {
      NotificationManagerCompat.from(context).notify(notificationId, notification)
    } catch (_: SecurityException) {
      // Permission revoked between check and notify
    }
  }
}
