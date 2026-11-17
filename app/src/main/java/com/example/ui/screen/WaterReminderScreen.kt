package com.example.ui.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.MedicationSlot
import com.example.notifications.MedReminderConfig
import com.example.notifications.ReminderSlotId
import com.example.notifications.WaterReminderConfig
import com.example.notifications.WaterReminderNotifier
import com.example.ui.DailyCheckUiState
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.IvoryBackground
import com.example.ui.theme.MedCompletedGreen
import com.example.ui.theme.MedCompletedGreenDark
import com.example.ui.theme.MedCompletedGreenSoft
import com.example.ui.theme.MorningAmber
import com.example.ui.theme.WarmCardSurface
import com.example.ui.theme.WarmHeaderSurface
import com.example.ui.theme.WarmStoneBorder
import com.example.ui.theme.WaterCardBorder
import com.example.ui.theme.WaterCardSurface
import com.example.ui.theme.WaterPrimaryBlue

private sealed class PendingPermissionAction {
  data class EnableMed(val slot: MedicationSlot) : PendingPermissionAction()

  data class EnableWater(val slotId: ReminderSlotId) : PendingPermissionAction()

  data object TestMed : PendingPermissionAction()

  data object TestWater : PendingPermissionAction()
}

private sealed class EditingTimeTarget {
  data class Medication(val config: MedReminderConfig) : EditingTimeTarget()

  data class Water(val config: WaterReminderConfig) : EditingTimeTarget()
}

@Composable
fun WaterReminderScreen(
  uiState: DailyCheckUiState,
  onToggleMedReminder: (MedicationSlot, Boolean) -> Unit,
  onUpdateMedReminderTime: (MedicationSlot, Int, Int) -> Unit,
  onSendTestMedReminder: () -> Unit,
  onToggleReminder: (ReminderSlotId, Boolean) -> Unit,
  onUpdateReminderTime: (ReminderSlotId, Int, Int) -> Unit,
  onSendTestReminder: () -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(0.dp),
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  var hasPermission by remember {
    mutableStateOf(WaterReminderNotifier.hasNotificationPermission(context))
  }
  var pendingAction by remember { mutableStateOf<PendingPermissionAction?>(null) }
  var medTestSentVisible by remember { mutableStateOf(false) }
  var waterTestSentVisible by remember { mutableStateOf(false) }
  var editingTarget by remember { mutableStateOf<EditingTimeTarget?>(null) }

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasPermission = granted || WaterReminderNotifier.hasNotificationPermission(context)
      if (hasPermission) {
        when (val action = pendingAction) {
          is PendingPermissionAction.EnableMed -> onToggleMedReminder(action.slot, true)
          is PendingPermissionAction.EnableWater -> onToggleReminder(action.slotId, true)
          PendingPermissionAction.TestMed -> {
            onSendTestMedReminder()
            medTestSentVisible = true
          }
          PendingPermissionAction.TestWater -> {
            onSendTestReminder()
            waterTestSentVisible = true
          }
          null -> Unit
        }
      }
      pendingAction = null
    }

  Box(
    modifier = modifier.fillMaxSize().background(IvoryBackground).padding(contentPadding),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .widthIn(max = 600.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 18.dp, vertical = 14.dp)
          .testTag("water_reminder_screen"),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      // 1. Header Card
      ReminderHeaderCard(activeCount = uiState.activeReminderCount)

      // 2. Notification Permission Card (Android 13+ when not yet granted)
      if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Surface(
          modifier = Modifier.fillMaxWidth().testTag("notification_permission_card"),
          shape = RoundedCornerShape(22.dp),
          color = WarmCardSurface,
          border = BorderStroke(2.dp, MorningAmber),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = stringResource(id = R.string.reminder_permission_needed),
              fontSize = 22.sp,
              lineHeight = 30.sp,
              fontWeight = FontWeight.SemiBold,
              color = InkPrimary,
            )
            Button(
              onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
              modifier =
                Modifier.fillMaxWidth()
                  .heightIn(min = 64.dp)
                  .testTag("request_notification_permission_button"),
              shape = RoundedCornerShape(18.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = MorningAmber,
                  contentColor = Color.White,
                ),
            ) {
              Text(
                text = stringResource(id = R.string.reminder_permission_button),
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }

      // 3. Daily Medication Reminders Section (아침 약 · 점심 약 · 저녁 약)
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(horizontal = 4.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.Medication,
            contentDescription = null,
            tint = MedCompletedGreenDark,
            modifier = Modifier.size(28.dp),
          )
          Text(
            text = stringResource(id = R.string.med_reminders_section_title),
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
            color = InkPrimary,
          )
        }

        uiState.medReminders.forEach { config ->
          val slotLabel =
            when (config.slot) {
              MedicationSlot.MORNING -> stringResource(id = R.string.med_reminder_morning)
              MedicationSlot.LUNCH -> stringResource(id = R.string.med_reminder_lunch)
              MedicationSlot.DINNER -> stringResource(id = R.string.med_reminder_dinner)
            }
          val tagSuffix = config.slot.name.lowercase()
          ReminderItemCard(
            title = slotLabel,
            hour = config.hour,
            minute = config.minute,
            enabled = config.enabled,
            activeSurfaceColor = MedCompletedGreenSoft,
            activeBorderColor = MedCompletedGreen,
            cardTag = "med_reminder_card_$tagSuffix",
            toggleTag = "med_reminder_toggle_$tagSuffix",
            timeButtonTag = "med_reminder_time_button_$tagSuffix",
            onToggle = { enable ->
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              if (enable && !hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pendingAction = PendingPermissionAction.EnableMed(config.slot)
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
              } else {
                onToggleMedReminder(config.slot, enable)
              }
            },
            onChangeTime = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              editingTarget = EditingTimeTarget.Medication(config)
            },
          )
        }

        Button(
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              pendingAction = PendingPermissionAction.TestMed
              permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              onSendTestMedReminder()
              medTestSentVisible = true
            }
          },
          modifier =
            Modifier.fillMaxWidth().heightIn(min = 72.dp).testTag("test_med_notification_button"),
          shape = RoundedCornerShape(20.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MedCompletedGreen,
              contentColor = Color.White,
            ),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = stringResource(id = R.string.med_reminder_test_button),
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
          )
        }

        AnimatedVisibility(visible = medTestSentVisible) {
          TestFeedbackBanner(
            text = stringResource(id = R.string.med_reminder_test_sent),
            testTag = "test_med_notification_feedback",
          )
        }
      }

      // 4. Water Intake Reminders Section
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(horizontal = 4.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.WaterDrop,
            contentDescription = null,
            tint = WaterPrimaryBlue,
            modifier = Modifier.size(28.dp),
          )
          Text(
            text = stringResource(id = R.string.water_reminders_section_title),
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
            color = InkPrimary,
          )
        }

        uiState.waterReminders.forEach { config ->
          val slotLabel =
            when (config.slotId) {
              ReminderSlotId.MORNING -> stringResource(id = R.string.reminder_slot_morning)
              ReminderSlotId.AFTERNOON -> stringResource(id = R.string.reminder_slot_afternoon)
              ReminderSlotId.EVENING -> stringResource(id = R.string.reminder_slot_evening)
            }
          val tagSuffix = config.slotId.name.lowercase()
          ReminderItemCard(
            title = slotLabel,
            hour = config.hour,
            minute = config.minute,
            enabled = config.enabled,
            activeSurfaceColor = WaterCardSurface,
            activeBorderColor = WaterPrimaryBlue,
            cardTag = "reminder_card_$tagSuffix",
            toggleTag = "reminder_toggle_$tagSuffix",
            timeButtonTag = "reminder_time_button_$tagSuffix",
            onToggle = { enable ->
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              if (enable && !hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pendingAction = PendingPermissionAction.EnableWater(config.slotId)
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
              } else {
                onToggleReminder(config.slotId, enable)
              }
            },
            onChangeTime = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              editingTarget = EditingTimeTarget.Water(config)
            },
          )
        }

        Button(
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              pendingAction = PendingPermissionAction.TestWater
              permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              onSendTestReminder()
              waterTestSentVisible = true
            }
          },
          modifier =
            Modifier.fillMaxWidth().heightIn(min = 72.dp).testTag("test_notification_button"),
          shape = RoundedCornerShape(20.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = WaterPrimaryBlue,
              contentColor = Color.White,
            ),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = stringResource(id = R.string.reminder_test_button),
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
          )
        }

        AnimatedVisibility(visible = waterTestSentVisible) {
          TestFeedbackBanner(
            text = stringResource(id = R.string.reminder_test_sent),
            testTag = "test_notification_feedback",
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
    }
  }

  editingTarget?.let { target ->
    val (initialHour, initialMinute) =
      when (target) {
        is EditingTimeTarget.Medication -> target.config.hour to target.config.minute
        is EditingTimeTarget.Water -> target.config.hour to target.config.minute
      }

    SeniorTimePickerDialog(
      initialHour = initialHour,
      initialMinute = initialMinute,
      onDismiss = { editingTarget = null },
      onSave = { newHour, newMinute ->
        editingTarget = null
        when (target) {
          is EditingTimeTarget.Medication ->
            onUpdateMedReminderTime(target.config.slot, newHour, newMinute)
          is EditingTimeTarget.Water ->
            onUpdateReminderTime(target.config.slotId, newHour, newMinute)
        }
        if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
      },
    )
  }
}

@Composable
private fun TestFeedbackBanner(text: String, testTag: String) {
  Surface(
    modifier = Modifier.fillMaxWidth().testTag(testTag),
    shape = RoundedCornerShape(18.dp),
    color = MedCompletedGreenSoft,
    border = BorderStroke(1.5.dp, MedCompletedGreen),
  ) {
    Text(
      text = text,
      fontSize = 22.sp,
      lineHeight = 30.sp,
      fontWeight = FontWeight.Bold,
      color = MedCompletedGreenDark,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    )
  }
}

@Composable
private fun ReminderHeaderCard(activeCount: Int) {
  Surface(
    modifier = Modifier.fillMaxWidth().testTag("reminder_header_card"),
    shape = RoundedCornerShape(24.dp),
    color = WarmHeaderSurface,
    border = BorderStroke(1.5.dp, WarmStoneBorder),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.NotificationsActive,
          contentDescription = null,
          tint = MedCompletedGreenDark,
          modifier = Modifier.size(30.dp),
        )
        Text(
          text = stringResource(id = R.string.reminders_title),
          fontSize = 28.sp,
          lineHeight = 36.sp,
          fontWeight = FontWeight.Bold,
          color = InkPrimary,
        )
      }

      Text(
        text = stringResource(id = R.string.reminders_subtitle),
        fontSize = 22.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Medium,
        color = InkSecondary,
      )

      val hasActive = activeCount > 0
      Surface(
        shape = RoundedCornerShape(50),
        color = if (hasActive) MedCompletedGreen else WarmCardSurface,
        border = BorderStroke(1.5.dp, if (hasActive) MedCompletedGreenDark else WarmStoneBorder),
      ) {
        Text(
          text =
            if (hasActive) stringResource(id = R.string.reminders_active_count_format, activeCount)
            else stringResource(id = R.string.reminders_none_active),
          fontSize = 22.sp,
          lineHeight = 28.sp,
          fontWeight = FontWeight.Bold,
          color = if (hasActive) Color.White else InkPrimary,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
      }
    }
  }
}

@Composable
private fun ReminderItemCard(
  title: String,
  hour: Int,
  minute: Int,
  enabled: Boolean,
  activeSurfaceColor: Color,
  activeBorderColor: Color,
  cardTag: String,
  toggleTag: String,
  timeButtonTag: String,
  onToggle: (Boolean) -> Unit,
  onChangeTime: () -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth().testTag(cardTag),
    shape = RoundedCornerShape(22.dp),
    color = if (enabled) activeSurfaceColor else WarmCardSurface,
    border = BorderStroke(2.dp, if (enabled) activeBorderColor else WarmStoneBorder),
    shadowElevation = 2.dp,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = title,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = InkSecondary,
          )
          Text(
            text = formatKoreanTime(hour, minute),
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            color = InkPrimary,
          )
        }

        Button(
          onClick = { onToggle(!enabled) },
          modifier = Modifier.heightIn(min = 58.dp).testTag(toggleTag),
          shape = RoundedCornerShape(50),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = if (enabled) MedCompletedGreen else WarmHeaderSurface,
              contentColor = if (enabled) Color.White else InkPrimary,
            ),
        ) {
          Icon(
            imageVector =
              if (enabled) Icons.Filled.NotificationsActive else Icons.Rounded.NotificationsOff,
            contentDescription = null,
            modifier = Modifier.size(26.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text =
              if (enabled) stringResource(id = R.string.reminder_on)
              else stringResource(id = R.string.reminder_off),
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      OutlinedButton(
        onClick = onChangeTime,
        modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp).testTag(timeButtonTag),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, if (enabled) activeBorderColor.copy(alpha = 0.55f) else WarmStoneBorder),
        colors =
          ButtonDefaults.outlinedButtonColors(
            containerColor = WarmCardSurface,
            contentColor = InkPrimary,
          ),
      ) {
        Icon(
          imageVector = Icons.Rounded.AccessTime,
          contentDescription = null,
          modifier = Modifier.size(26.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(id = R.string.reminder_change_time),
          fontSize = 22.sp,
          lineHeight = 28.sp,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
private fun SeniorTimePickerDialog(
  initialHour: Int,
  initialMinute: Int,
  onDismiss: () -> Unit,
  onSave: (Int, Int) -> Unit,
) {
  var hour by remember { mutableIntStateOf(initialHour) }
  var minute by remember { mutableIntStateOf(initialMinute) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = WarmCardSurface,
    shape = RoundedCornerShape(26.dp),
    title = {
      Text(
        text = stringResource(id = R.string.time_picker_title),
        fontSize = 26.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        color = InkPrimary,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          color = WaterCardSurface,
          border = BorderStroke(1.5.dp, WaterCardBorder),
        ) {
          Text(
            text = formatKoreanTime(hour, minute),
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            color = WaterPrimaryBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
          )
        }

        // Hour stepper
        TimeStepperRow(
          valueText = "$hour ${stringResource(id = R.string.time_picker_hour_label)}",
          onMinus = { hour = (hour + 23) % 24 },
          onPlus = { hour = (hour + 1) % 24 },
          minusTag = "time_picker_hour_minus",
          plusTag = "time_picker_hour_plus",
        )

        // Minute stepper (5-minute increments for senior ease)
        TimeStepperRow(
          valueText = "$minute ${stringResource(id = R.string.time_picker_minute_label)}",
          onMinus = { minute = (minute + 55) % 60 },
          onPlus = { minute = (minute + 5) % 60 },
          minusTag = "time_picker_minute_minus",
          plusTag = "time_picker_minute_plus",
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(hour, minute) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).testTag("time_picker_save_button"),
        shape = RoundedCornerShape(18.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MedCompletedGreen,
            contentColor = Color.White,
          ),
      ) {
        Text(
          text = stringResource(id = R.string.time_picker_save),
          fontSize = 22.sp,
          lineHeight = 28.sp,
          fontWeight = FontWeight.Bold,
        )
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, WarmStoneBorder),
        colors =
          ButtonDefaults.outlinedButtonColors(
            containerColor = WarmHeaderSurface,
            contentColor = InkPrimary,
          ),
      ) {
        Text(
          text = stringResource(id = R.string.clear_dialog_cancel),
          fontSize = 22.sp,
          lineHeight = 28.sp,
          fontWeight = FontWeight.Bold,
        )
      }
    },
  )
}

@Composable
private fun TimeStepperRow(
  valueText: String,
  onMinus: () -> Unit,
  onPlus: () -> Unit,
  minusTag: String,
  plusTag: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    OutlinedButton(
      onClick = onMinus,
      modifier = Modifier.size(64.dp).testTag(minusTag),
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(1.5.dp, WarmStoneBorder),
      contentPadding = PaddingValues(0.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.Remove,
        contentDescription = "-",
        tint = InkPrimary,
        modifier = Modifier.size(32.dp),
      )
    }

    Text(
      text = valueText,
      fontSize = 26.sp,
      lineHeight = 34.sp,
      fontWeight = FontWeight.Bold,
      color = InkPrimary,
    )

    OutlinedButton(
      onClick = onPlus,
      modifier = Modifier.size(64.dp).testTag(plusTag),
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(1.5.dp, WarmStoneBorder),
      contentPadding = PaddingValues(0.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.Add,
        contentDescription = "+",
        tint = InkPrimary,
        modifier = Modifier.size(32.dp),
      )
    }
  }
}

fun formatKoreanTime(hour24: Int, minute: Int): String {
  val period = if (hour24 < 12) "오전" else "오후"
  val hour12 =
    when {
      hour24 == 0 -> 12
      hour24 > 12 -> hour24 - 12
      else -> hour24
    }
  return if (minute == 0) "$period ${hour12}시 정각" else "$period ${hour12}시 ${minute}분"
}
