package com.example.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.MedicationSlot
import com.example.ui.CalendarDayUi
import com.example.ui.DailyCheckUiState
import com.example.ui.theme.DinnerIndigo
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.IvoryBackground
import com.example.ui.theme.LunchCoral
import com.example.ui.theme.MedCompletedGreen
import com.example.ui.theme.MedCompletedGreenDark
import com.example.ui.theme.MedCompletedGreenSoft
import com.example.ui.theme.MorningAmber
import com.example.ui.theme.ResetConfirmRed
import com.example.ui.theme.WarmCardSurface
import com.example.ui.theme.WarmHeaderSurface
import com.example.ui.theme.WarmStoneBorder
import com.example.ui.theme.WaterCardBorder
import com.example.ui.theme.WaterCardSurface
import com.example.ui.theme.WaterPrimaryBlue

private val WeekdayLabels = listOf("일", "월", "화", "수", "목", "금", "토")

@Composable
fun PastHistoryCalendarScreen(
  uiState: DailyCheckUiState,
  onNavigateBack: () -> Unit,
  onSelectDate: (String) -> Unit,
  onPreviousMonth: () -> Unit,
  onNextMonth: () -> Unit,
  onToggleMedicationForDate: (String, MedicationSlot) -> Unit,
  onIncrementWaterForDate: (String) -> Unit,
  onDecrementWaterForDate: (String) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(0.dp),
) {
  val haptic = LocalHapticFeedback.current

  Box(
    modifier = modifier.fillMaxSize().background(IvoryBackground).padding(contentPadding),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .widthIn(max = 600.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 14.dp)
          .testTag("calendar_history_screen"),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // 1. Top Back Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedButton(
          onClick = onNavigateBack,
          modifier = Modifier.heightIn(min = 56.dp).testTag("calendar_back_button"),
          shape = RoundedCornerShape(18.dp),
          border = BorderStroke(1.5.dp, WarmStoneBorder),
          colors =
            ButtonDefaults.outlinedButtonColors(
              containerColor = WarmCardSurface,
              contentColor = InkPrimary,
            ),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(id = R.string.calendar_back_button),
            modifier = Modifier.size(28.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(id = R.string.calendar_back_button),
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
          )
        }

        Text(
          text = stringResource(id = R.string.calendar_screen_title),
          fontSize = 26.sp,
          lineHeight = 34.sp,
          fontWeight = FontWeight.Bold,
          color = InkPrimary,
        )
      }

      // 2. Month Calendar Card
      Surface(
        modifier = Modifier.fillMaxWidth().testTag("calendar_month_card"),
        shape = RoundedCornerShape(24.dp),
        color = WarmCardSurface,
        border = BorderStroke(2.dp, WarmStoneBorder),
        shadowElevation = 2.dp,
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          // Month selector row
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            OutlinedButton(
              onClick = onPreviousMonth,
              modifier = Modifier.size(54.dp).testTag("calendar_prev_month"),
              shape = RoundedCornerShape(16.dp),
              border = BorderStroke(1.5.dp, WarmStoneBorder),
              contentPadding = PaddingValues(0.dp),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = stringResource(id = R.string.calendar_prev_month),
                tint = InkPrimary,
                modifier = Modifier.size(34.dp),
              )
            }

            Text(
              text = uiState.calendarMonthLabel,
              fontSize = 26.sp,
              lineHeight = 34.sp,
              fontWeight = FontWeight.Bold,
              color = InkPrimary,
              modifier = Modifier.testTag("calendar_month_label"),
            )

            OutlinedButton(
              onClick = onNextMonth,
              enabled = uiState.canGoNextCalendarMonth,
              modifier = Modifier.size(54.dp).testTag("calendar_next_month"),
              shape = RoundedCornerShape(16.dp),
              border =
                BorderStroke(
                  1.5.dp,
                  if (uiState.canGoNextCalendarMonth) WarmStoneBorder
                  else WarmStoneBorder.copy(alpha = 0.45f),
                ),
              contentPadding = PaddingValues(0.dp),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = stringResource(id = R.string.calendar_next_month),
                tint =
                  if (uiState.canGoNextCalendarMonth) InkPrimary
                  else InkSecondary.copy(alpha = 0.35f),
                modifier = Modifier.size(34.dp),
              )
            }
          }

          // Weekday header row
          Row(modifier = Modifier.fillMaxWidth()) {
            WeekdayLabels.forEachIndexed { index, label ->
              val labelColor =
                when (index) {
                  0 -> ResetConfirmRed
                  6 -> WaterPrimaryBlue
                  else -> InkSecondary
                }
              Text(
                text = label,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
              )
            }
          }

          // Calendar day rows (7 columns)
          val rows = uiState.calendarCells.chunked(7)
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { rowCells ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                for (col in 0 until 7) {
                  val cell = rowCells.getOrNull(col)
                  if (cell == null) {
                    Spacer(modifier = Modifier.weight(1f).heightIn(min = 52.dp))
                  } else {
                    CalendarDayCell(
                      day = cell,
                      modifier = Modifier.weight(1f),
                      onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectDate(cell.dateKey)
                      },
                    )
                  }
                }
              }
            }
          }

          // Legend
          Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(MedCompletedGreen))
              Text(
                text = stringResource(id = R.string.calendar_legend_all),
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Medium,
                color = InkSecondary,
              )
            }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(MorningAmber))
              Text(
                text = stringResource(id = R.string.calendar_legend_partial),
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Medium,
                color = InkSecondary,
              )
            }
          }
        }
      }

      // 3. Selected Date Medication & Water Editor Card
      SelectedDateEditorCard(
        dateLabel = uiState.selectedCalendarDateLabel,
        selectedDateKey = uiState.selectedCalendarDateKey,
        morningTaken = uiState.selectedCalendarRecord.morningMedTaken,
        lunchTaken = uiState.selectedCalendarRecord.lunchMedTaken,
        dinnerTaken = uiState.selectedCalendarRecord.dinnerMedTaken,
        waterCups = uiState.selectedCalendarRecord.waterCups,
        onToggleMedication = { dateKey, slot ->
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onToggleMedicationForDate(dateKey, slot)
        },
        onIncrementWater = { dateKey ->
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onIncrementWaterForDate(dateKey)
        },
        onDecrementWater = { dateKey ->
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onDecrementWaterForDate(dateKey)
        },
      )
    }
  }
}

@Composable
private fun CalendarDayCell(
  day: CalendarDayUi,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val bgColor =
    when {
      day.isSelected -> WarmHeaderSurface
      day.takenMedCount >= 3 -> MedCompletedGreenSoft
      day.takenMedCount > 0 -> MorningAmber.copy(alpha = 0.14f)
      else -> Color.Transparent
    }

  val borderStroke =
    when {
      day.isSelected -> BorderStroke(2.5.dp, InkPrimary)
      day.isToday -> BorderStroke(1.5.dp, MedCompletedGreenDark)
      else -> null
    }

  val dotColor =
    when {
      day.takenMedCount >= 3 -> MedCompletedGreen
      day.takenMedCount > 0 -> MorningAmber
      else -> Color.Transparent
    }

  Surface(
    modifier =
      modifier
        .heightIn(min = 52.dp)
        .clip(RoundedCornerShape(14.dp))
        .clickable(enabled = !day.isFuture, onClick = onClick)
        .testTag("calendar_day_${day.dateKey}"),
    shape = RoundedCornerShape(14.dp),
    color = bgColor,
    border = borderStroke,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = day.dayOfMonth.toString(),
        fontSize = 22.sp,
        lineHeight = 26.sp,
        fontWeight = if (day.isSelected || day.isToday) FontWeight.Bold else FontWeight.Medium,
        color = if (day.isFuture) InkSecondary.copy(alpha = 0.35f) else InkPrimary,
        textAlign = TextAlign.Center,
      )

      Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
    }
  }
}

@Composable
private fun SelectedDateEditorCard(
  dateLabel: String,
  selectedDateKey: String,
  morningTaken: Boolean,
  lunchTaken: Boolean,
  dinnerTaken: Boolean,
  waterCups: Int,
  onToggleMedication: (String, MedicationSlot) -> Unit,
  onIncrementWater: (String) -> Unit,
  onDecrementWater: (String) -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth().testTag("calendar_selected_editor_card"),
    shape = RoundedCornerShape(24.dp),
    color = WarmHeaderSurface,
    border = BorderStroke(2.dp, WarmStoneBorder),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = dateLabel,
        fontSize = 25.sp,
        lineHeight = 33.sp,
        fontWeight = FontWeight.Bold,
        color = InkPrimary,
        modifier = Modifier.testTag("calendar_selected_date_title"),
      )

      CalendarMedToggleRow(
        label = stringResource(id = R.string.med_morning),
        icon = Icons.Rounded.WbTwilight,
        uncheckedAccentColor = MorningAmber,
        isChecked = morningTaken,
        testTag = "calendar_med_morning",
        onClick = { onToggleMedication(selectedDateKey, MedicationSlot.MORNING) },
      )

      CalendarMedToggleRow(
        label = stringResource(id = R.string.med_lunch),
        icon = Icons.Rounded.LightMode,
        uncheckedAccentColor = LunchCoral,
        isChecked = lunchTaken,
        testTag = "calendar_med_lunch",
        onClick = { onToggleMedication(selectedDateKey, MedicationSlot.LUNCH) },
      )

      CalendarMedToggleRow(
        label = stringResource(id = R.string.med_dinner),
        icon = Icons.Rounded.NightsStay,
        uncheckedAccentColor = DinnerIndigo,
        isChecked = dinnerTaken,
        testTag = "calendar_med_dinner",
        onClick = { onToggleMedication(selectedDateKey, MedicationSlot.DINNER) },
      )

      // Past-day water adjustment row
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = WaterCardSurface,
        border = BorderStroke(1.5.dp, WaterCardBorder),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.WaterDrop,
              contentDescription = null,
              tint = WaterPrimaryBlue,
              modifier = Modifier.size(28.dp),
            )
            Text(
              text = stringResource(id = R.string.calendar_selected_water_format, waterCups),
              fontSize = 22.sp,
              lineHeight = 28.sp,
              fontWeight = FontWeight.Bold,
              color = InkPrimary,
              modifier = Modifier.testTag("calendar_water_text"),
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
              onClick = { onDecrementWater(selectedDateKey) },
              enabled = waterCups > 0,
              modifier = Modifier.size(52.dp).testTag("calendar_water_minus_$selectedDateKey"),
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.5.dp, WaterCardBorder),
              contentPadding = PaddingValues(0.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Remove,
                contentDescription = "-",
                tint = if (waterCups > 0) InkPrimary else InkSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(28.dp),
              )
            }

            OutlinedButton(
              onClick = { onIncrementWater(selectedDateKey) },
              modifier = Modifier.size(52.dp).testTag("calendar_water_plus_$selectedDateKey"),
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.5.dp, WaterPrimaryBlue),
              contentPadding = PaddingValues(0.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "+",
                tint = WaterPrimaryBlue,
                modifier = Modifier.size(28.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CalendarMedToggleRow(
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
      animationSpec = tween(durationMillis = 200),
      label = "calMedBg",
    )
  val contentColor = if (isChecked) Color.White else InkPrimary
  val stateDesc =
    if (isChecked) stringResource(id = R.string.cd_med_checked)
    else stringResource(id = R.string.cd_med_unchecked)

  Surface(
    onClick = onClick,
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = 74.dp)
        .testTag(testTag)
        .semantics { stateDescription = stateDesc },
    shape = RoundedCornerShape(20.dp),
    color = containerColor,
    border = BorderStroke(2.dp, if (isChecked) MedCompletedGreenDark else WarmStoneBorder),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (isChecked) Color.White else uncheckedAccentColor,
          modifier = Modifier.size(30.dp),
        )
        Text(
          text = label,
          fontSize = 24.sp,
          lineHeight = 32.sp,
          fontWeight = FontWeight.Bold,
          color = contentColor,
        )
      }

      if (isChecked) {
        Surface(shape = RoundedCornerShape(50), color = MedCompletedGreenSoft) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.CheckCircle,
              contentDescription = null,
              tint = MedCompletedGreenDark,
              modifier = Modifier.size(26.dp),
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
            modifier = Modifier.size(28.dp),
          )
        }
      }
    }
  }
}
