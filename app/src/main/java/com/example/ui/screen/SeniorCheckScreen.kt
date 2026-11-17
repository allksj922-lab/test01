package com.example.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.MedicationSlot
import com.example.ui.DailyCheckUiState
import com.example.ui.components.WaterCupIllustration
import com.example.ui.theme.DinnerIndigo
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.LunchCoral
import com.example.ui.theme.MedCompletedGreen
import com.example.ui.theme.MedCompletedGreenDark
import com.example.ui.theme.MedCompletedGreenSoft
import com.example.ui.theme.MorningAmber
import com.example.ui.theme.ResetButtonBorder
import com.example.ui.theme.ResetButtonSurface
import com.example.ui.theme.ResetButtonText
import com.example.ui.theme.ResetConfirmRed
import com.example.ui.theme.WarmCardSurface
import com.example.ui.theme.WarmHeaderSurface
import com.example.ui.theme.WarmStoneBorder
import com.example.ui.theme.WaterCardBorder
import com.example.ui.theme.WaterCardSurface
import com.example.ui.theme.WaterCupLiquid
import com.example.ui.theme.WaterPrimaryBlue

@Composable
fun SeniorCheckScreen(
  uiState: DailyCheckUiState,
  onToggleMedication: (MedicationSlot) -> Unit,
  onAddWaterCup: () -> Unit,
  onRemoveWaterCup: () -> Unit,
  onClearTodayRecord: () -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(0.dp),
) {
  val haptic = LocalHapticFeedback.current
  var showClearConfirmDialog by remember { mutableStateOf(false) }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(contentPadding),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .widthIn(max = 600.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 18.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // 1. Top Date Header Card
        TodayDateHeader(
          formattedDate = uiState.formattedDate,
          dayOfWeekLabel = uiState.dayOfWeekLabel,
          takenMedCount = uiState.takenMedCount,
        )

        // 2. Medication Section (Vertical Big Buttons)
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
          Text(
            text = stringResource(id = R.string.section_medications),
            style = MaterialTheme.typography.titleLarge,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = InkPrimary,
            modifier = Modifier.padding(horizontal = 4.dp),
          )

          MedicationCheckButton(
            label = stringResource(id = R.string.med_morning),
            icon = Icons.Rounded.WbTwilight,
            uncheckedAccentColor = MorningAmber,
            isChecked = uiState.morningMedTaken,
            testTag = "med_button_morning",
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onToggleMedication(MedicationSlot.MORNING)
            },
          )

          MedicationCheckButton(
            label = stringResource(id = R.string.med_lunch),
            icon = Icons.Rounded.LightMode,
            uncheckedAccentColor = LunchCoral,
            isChecked = uiState.lunchMedTaken,
            testTag = "med_button_lunch",
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onToggleMedication(MedicationSlot.LUNCH)
            },
          )

          MedicationCheckButton(
            label = stringResource(id = R.string.med_dinner),
            icon = Icons.Rounded.NightsStay,
            uncheckedAccentColor = DinnerIndigo,
            isChecked = uiState.dinnerMedTaken,
            testTag = "med_button_dinner",
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onToggleMedication(MedicationSlot.DINNER)
            },
          )
        }

        // 3. Water Intake Section (Bottom)
        WaterIntakeCard(
          waterCups = uiState.waterCups,
          dailyWaterGoal = uiState.dailyWaterGoal,
          onAddWaterCup = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onAddWaterCup()
          },
          onRemoveWaterCup = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onRemoveWaterCup()
          },
        )

      // 4. Clear Today's Record Button
      ClearTodayRecordButton(
        enabled = uiState.hasAnyRecord,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          showClearConfirmDialog = true
        },
      )
    }
  }

  if (showClearConfirmDialog) {
    ClearRecordConfirmDialog(
      onConfirm = {
        showClearConfirmDialog = false
        onClearTodayRecord()
      },
      onDismiss = { showClearConfirmDialog = false },
    )
  }
}

@Composable
private fun TodayDateHeader(
  formattedDate: String,
  dayOfWeekLabel: String,
  takenMedCount: Int,
) {
  Surface(
    modifier = Modifier.fillMaxWidth().testTag("today_date_header"),
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
          imageVector = Icons.Filled.CalendarToday,
          contentDescription = null,
          tint = MedCompletedGreenDark,
          modifier = Modifier.size(28.dp),
        )
        Text(
          text = formattedDate,
          fontSize = 28.sp,
          lineHeight = 36.sp,
          fontWeight = FontWeight.Bold,
          color = InkPrimary,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = dayOfWeekLabel,
          fontSize = 22.sp,
          lineHeight = 30.sp,
          fontWeight = FontWeight.SemiBold,
          color = InkSecondary,
        )

        val allDone = takenMedCount == 3
        Surface(
          shape = RoundedCornerShape(50),
          color = if (allDone) MedCompletedGreen else WarmCardSurface,
          border = BorderStroke(1.5.dp, if (allDone) MedCompletedGreenDark else WarmStoneBorder),
        ) {
          Text(
            text = stringResource(id = R.string.med_progress_format, takenMedCount),
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            color = if (allDone) Color.White else InkPrimary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun MedicationCheckButton(
  label: String,
  icon: ImageVector,
  uncheckedAccentColor: Color,
  isChecked: Boolean,
  testTag: String,
  onClick: () -> Unit,
) {
  val containerColor by
    animateColorAsState(
      targetValue = if (isChecked) MedCompletedGreen else WarmCardSurface,
      animationSpec = tween(durationMillis = 220),
      label = "medContainerColor",
    )
  val borderColor by
    animateColorAsState(
      targetValue = if (isChecked) MedCompletedGreenDark else WarmStoneBorder,
      animationSpec = tween(durationMillis = 220),
      label = "medBorderColor",
    )
  val contentColor by
    animateColorAsState(
      targetValue = if (isChecked) Color.White else InkPrimary,
      animationSpec = tween(durationMillis = 220),
      label = "medContentColor",
    )

  val stateLabel =
    if (isChecked) stringResource(id = R.string.cd_med_checked)
    else stringResource(id = R.string.cd_med_unchecked)

  Surface(
    onClick = onClick,
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = 82.dp)
        .testTag(testTag)
        .semantics { stateDescription = stateLabel },
    shape = RoundedCornerShape(22.dp),
    color = containerColor,
    border = BorderStroke(2.dp, borderColor),
    shadowElevation = if (isChecked) 2.dp else 4.dp,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.weight(1f),
      ) {
        Box(
          modifier =
            Modifier.size(50.dp)
              .clip(CircleShape)
              .background(
                if (isChecked) Color.White.copy(alpha = 0.22f)
                else uncheckedAccentColor.copy(alpha = 0.14f)
              ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isChecked) Color.White else uncheckedAccentColor,
            modifier = Modifier.size(30.dp),
          )
        }

        Text(
          text = label,
          fontSize = 26.sp,
          lineHeight = 34.sp,
          fontWeight = FontWeight.Bold,
          color = contentColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      if (isChecked) {
        Surface(
          shape = RoundedCornerShape(50),
          color = MedCompletedGreenSoft,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.CheckCircle,
              contentDescription = null,
              tint = MedCompletedGreenDark,
              modifier = Modifier.size(28.dp),
            )
            Text(
              text = stringResource(id = R.string.status_taken),
              fontSize = 22.sp,
              lineHeight = 28.sp,
              fontWeight = FontWeight.Bold,
              color = MedCompletedGreenDark,
            )
          }
        }
      } else {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = stringResource(id = R.string.status_not_taken),
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Medium,
            color = InkSecondary,
          )
          Icon(
            imageVector = Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = InkSecondary,
            modifier = Modifier.size(30.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun WaterIntakeCard(
  waterCups: Int,
  dailyWaterGoal: Int,
  onAddWaterCup: () -> Unit,
  onRemoveWaterCup: () -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth().testTag("water_intake_card"),
    shape = RoundedCornerShape(24.dp),
    color = WaterCardSurface,
    border = BorderStroke(2.dp, WaterCardBorder),
    shadowElevation = 3.dp,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        WaterCupIllustration(waterCups = waterCups, goalCups = dailyWaterGoal)

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = stringResource(id = R.string.water_section_title),
            fontSize = 22.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold,
            color = InkSecondary,
          )

          Text(
            text = stringResource(id = R.string.water_count_format, waterCups),
            fontSize = 42.sp,
            lineHeight = 50.sp,
            fontWeight = FontWeight.Bold,
            color = WaterPrimaryBlue,
            modifier = Modifier.testTag("water_count_text"),
          )

          val goalReached = waterCups >= dailyWaterGoal
          Text(
            text =
              if (goalReached) stringResource(id = R.string.water_goal_reached)
              else stringResource(id = R.string.water_goal_format, dailyWaterGoal),
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = if (goalReached) FontWeight.Bold else FontWeight.Medium,
            color = if (goalReached) MedCompletedGreenDark else InkSecondary,
          )
        }
      }

      // Visual cup progress indicators (8 clear segments)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        for (index in 1..dailyWaterGoal) {
          val filled = index <= waterCups
          Box(
            modifier =
              Modifier.weight(1f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (filled) WaterCupLiquid else WaterCardBorder.copy(alpha = 0.55f))
          )
        }
      }

      Button(
        onClick = onAddWaterCup,
        modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp).testTag("add_water_button"),
        shape = RoundedCornerShape(20.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = WaterPrimaryBlue,
            contentColor = Color.White,
          ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.WaterDrop,
          contentDescription = null,
          modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = stringResource(id = R.string.water_add_button),
          fontSize = 23.sp,
          lineHeight = 30.sp,
          fontWeight = FontWeight.Bold,
        )
      }

      AnimatedVisibility(visible = waterCups > 0) {
        OutlinedButton(
          onClick = onRemoveWaterCup,
          modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp).testTag("remove_water_button"),
          shape = RoundedCornerShape(18.dp),
          border = BorderStroke(1.5.dp, WaterCardBorder),
          colors =
            ButtonDefaults.outlinedButtonColors(
              containerColor = WarmCardSurface,
              contentColor = InkSecondary,
            ),
        ) {
          Icon(
            imageVector = Icons.Rounded.RemoveCircleOutline,
            contentDescription = null,
            modifier = Modifier.size(26.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(id = R.string.water_undo_button),
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Medium,
          )
        }
      }
    }
  }
}

@Composable
private fun ClearTodayRecordButton(enabled: Boolean, onClick: () -> Unit) {
  OutlinedButton(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp).testTag("clear_today_button"),
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(1.5.dp, if (enabled) ResetButtonBorder else WarmStoneBorder),
    colors =
      ButtonDefaults.outlinedButtonColors(
        containerColor = if (enabled) ResetButtonSurface else WarmCardSurface,
        contentColor = ResetButtonText,
        disabledContainerColor = WarmCardSurface.copy(alpha = 0.65f),
        disabledContentColor = InkSecondary.copy(alpha = 0.55f),
      ),
  ) {
    Icon(
      imageVector = Icons.Rounded.DeleteSweep,
      contentDescription = null,
      modifier = Modifier.size(28.dp),
    )
    Spacer(modifier = Modifier.width(10.dp))
    Text(
      text = stringResource(id = R.string.clear_today_button),
      fontSize = 22.sp,
      lineHeight = 28.sp,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun ClearRecordConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = WarmCardSurface,
    shape = RoundedCornerShape(26.dp),
    title = {
      Text(
        text = stringResource(id = R.string.clear_dialog_title),
        fontSize = 26.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        color = InkPrimary,
      )
    },
    text = {
      Text(
        text = stringResource(id = R.string.clear_dialog_message),
        fontSize = 22.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Medium,
        color = InkSecondary,
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp).testTag("confirm_clear_button"),
        shape = RoundedCornerShape(18.dp),
        colors =
          ButtonDefaults.buttonColors(containerColor = ResetConfirmRed, contentColor = Color.White),
      ) {
        Text(
          text = stringResource(id = R.string.clear_dialog_confirm),
          fontSize = 22.sp,
          lineHeight = 28.sp,
          fontWeight = FontWeight.Bold,
        )
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp).testTag("cancel_clear_button"),
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
