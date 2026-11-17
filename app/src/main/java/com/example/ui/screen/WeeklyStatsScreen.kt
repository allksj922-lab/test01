package com.example.ui.screen

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.DailyCheckUiState
import com.example.ui.DailySummaryUi
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
import com.example.ui.theme.WaterCupLiquid
import com.example.ui.theme.WaterPrimaryBlue

@Composable
fun WeeklyStatsScreen(
  uiState: DailyCheckUiState,
  onOpenCalendarHistory: () -> Unit = {},
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(0.dp),
) {
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
          .testTag("weekly_stats_screen"),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      // 1. Weekly Overview Header
      WeeklySummaryHeaderCard(
        rangeLabel = uiState.weeklyRangeLabel,
        weeklyMedTakenTotal = uiState.weeklyMedTakenTotal,
        weeklyWaterTotal = uiState.weeklyWaterTotal,
        weeklyWaterAverage = uiState.weeklyWaterAverageFormatted,
        hasAnyWeeklyRecord = uiState.hasAnyWeeklyRecord,
      )

      // 2. Medication 7-Day Bar Chart
      WeeklyBarChartCard(
        title = stringResource(id = R.string.stats_med_chart_title),
        subtitle = stringResource(id = R.string.stats_med_chart_unit),
        days = uiState.weeklyDays,
        maxScale = 3,
        goalThreshold = 3,
        cardColor = WarmCardSurface,
        borderColor = WarmStoneBorder,
        accentColor = MedCompletedGreenDark,
        headerIcon = Icons.Filled.Medication,
        testTag = "weekly_med_chart",
        valueSelector = { it.takenMedCount },
        barColorSelector = { count ->
          when {
            count >= 3 -> MedCompletedGreen
            count > 0 -> MorningAmber
            else -> WarmStoneBorder.copy(alpha = 0.5f)
          }
        },
        valueTextColorSelector = { count ->
          when {
            count >= 3 -> MedCompletedGreenDark
            count > 0 -> InkPrimary
            else -> InkSecondary
          }
        },
      )

      // 3. Water Intake 7-Day Bar Chart
      val maxWaterInWeek = (uiState.weeklyDays.maxOfOrNull { it.waterCups } ?: 0)
      val waterScale = maxOf(uiState.dailyWaterGoal, maxWaterInWeek)
      WeeklyBarChartCard(
        title = stringResource(id = R.string.stats_water_chart_title),
        subtitle = stringResource(id = R.string.stats_water_chart_goal),
        days = uiState.weeklyDays,
        maxScale = waterScale,
        goalThreshold = uiState.dailyWaterGoal,
        cardColor = WaterCardSurface,
        borderColor = WaterCardBorder,
        accentColor = WaterPrimaryBlue,
        headerIcon = Icons.Filled.WaterDrop,
        testTag = "weekly_water_chart",
        valueSelector = { it.waterCups },
        barColorSelector = { cups ->
          when {
            cups >= uiState.dailyWaterGoal -> WaterPrimaryBlue
            cups > 0 -> WaterCupLiquid
            else -> WaterCardBorder.copy(alpha = 0.6f)
          }
        },
        valueTextColorSelector = { cups ->
          if (cups > 0) WaterPrimaryBlue else InkSecondary
        },
      )

      Button(
        onClick = onOpenCalendarHistory,
        modifier =
          Modifier.fillMaxWidth()
            .heightIn(min = 74.dp)
            .testTag("open_calendar_history_button"),
        shape = RoundedCornerShape(22.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MedCompletedGreen,
            contentColor = Color.White,
          ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.CalendarMonth,
          contentDescription = null,
          modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = stringResource(id = R.string.open_calendar_history_button),
          fontSize = 22.sp,
          lineHeight = 28.sp,
          fontWeight = FontWeight.Bold,
        )
      }

      Spacer(modifier = Modifier.height(6.dp))
    }
  }
}

@Composable
private fun WeeklySummaryHeaderCard(
  rangeLabel: String,
  weeklyMedTakenTotal: Int,
  weeklyWaterTotal: Int,
  weeklyWaterAverage: String,
  hasAnyWeeklyRecord: Boolean,
) {
  Surface(
    modifier = Modifier.fillMaxWidth().testTag("weekly_summary_card"),
    shape = RoundedCornerShape(24.dp),
    color = WarmHeaderSurface,
    border = BorderStroke(1.5.dp, WarmStoneBorder),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.BarChart,
          contentDescription = null,
          tint = MedCompletedGreenDark,
          modifier = Modifier.size(30.dp),
        )
        Text(
          text = stringResource(id = R.string.stats_title),
          fontSize = 28.sp,
          lineHeight = 36.sp,
          fontWeight = FontWeight.Bold,
          color = InkPrimary,
        )
      }

      Text(
        text = rangeLabel,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Medium,
        color = InkSecondary,
      )

      // Summary Badges
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MedCompletedGreenSoft,
        border = BorderStroke(1.dp, MedCompletedGreen.copy(alpha = 0.35f)),
      ) {
        Text(
          text = stringResource(id = R.string.stats_med_total_format, weeklyMedTakenTotal),
          fontSize = 22.sp,
          lineHeight = 30.sp,
          fontWeight = FontWeight.Bold,
          color = MedCompletedGreenDark,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
      }

      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = WaterCardSurface,
        border = BorderStroke(1.dp, WaterCardBorder),
      ) {
        Text(
          text =
            stringResource(
              id = R.string.stats_water_total_format,
              weeklyWaterTotal,
              weeklyWaterAverage,
            ),
          fontSize = 22.sp,
          lineHeight = 30.sp,
          fontWeight = FontWeight.Bold,
          color = WaterPrimaryBlue,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
      }

      if (!hasAnyWeeklyRecord) {
        Text(
          text = stringResource(id = R.string.stats_empty_hint),
          fontSize = 22.sp,
          lineHeight = 30.sp,
          fontWeight = FontWeight.Medium,
          color = InkSecondary,
        )
      }
    }
  }
}

@Composable
private fun WeeklyBarChartCard(
  title: String,
  subtitle: String,
  days: List<DailySummaryUi>,
  maxScale: Int,
  goalThreshold: Int,
  cardColor: Color,
  borderColor: Color,
  accentColor: Color,
  headerIcon: androidx.compose.ui.graphics.vector.ImageVector,
  testTag: String,
  valueSelector: (DailySummaryUi) -> Int,
  barColorSelector: (Int) -> Color,
  valueTextColorSelector: (Int) -> Color,
) {
  Surface(
    modifier = Modifier.fillMaxWidth().testTag(testTag),
    shape = RoundedCornerShape(24.dp),
    color = cardColor,
    border = BorderStroke(2.dp, borderColor),
    shadowElevation = 2.dp,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = headerIcon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(28.dp),
          )
          Text(
            text = title,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
            color = InkPrimary,
          )
        }

        Text(
          text = subtitle,
          fontSize = 22.sp,
          lineHeight = 28.sp,
          fontWeight = FontWeight.Medium,
          color = InkSecondary,
        )
      }

      val chartHeight = 136.dp
      val safeScale = maxScale.coerceAtLeast(1)
      val goalRatio = (goalThreshold.toFloat() / safeScale).coerceIn(0f, 1f)

      Box(modifier = Modifier.fillMaxWidth()) {
        // Subtle dashed goal reference line behind the bars
        Canvas(
          modifier =
            Modifier.fillMaxWidth()
              .padding(top = 36.dp) // aligns below the value labels row
              .height(chartHeight)
        ) {
          val goalY = size.height * (1f - goalRatio)
          drawLine(
            color = accentColor.copy(alpha = 0.28f),
            start = Offset(0f, goalY),
            end = Offset(size.width, goalY),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.Bottom,
        ) {
          days.forEach { day ->
            val value = valueSelector(day)
            val targetRatio = (value.toFloat() / safeScale).coerceIn(0f, 1f)
            val animatedRatio by
              animateFloatAsState(
                targetValue = targetRatio,
                animationSpec =
                  spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                  ),
                label = "barRatio_${day.dateKey}",
              )

            val daySemantics = "${day.shortMonthDay} ${day.shortDayLabel}요일, $value"
            Column(
              modifier =
                Modifier.weight(1f).semantics(mergeDescendants = true) {
                  contentDescription = daySemantics
                },
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              // Value label above bar (>= 22sp)
              Text(
                text = value.toString(),
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                color = valueTextColorSelector(value),
                textAlign = TextAlign.Center,
              )

              // Vertical bar track + animated fill
              Box(
                modifier =
                  Modifier.fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .height(chartHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(borderColor.copy(alpha = 0.22f)),
                contentAlignment = Alignment.BottomCenter,
              ) {
                val barFraction = if (value > 0) maxOf(animatedRatio, 0.08f) else 0.04f
                Box(
                  modifier =
                    Modifier.fillMaxWidth()
                      .height(chartHeight * barFraction)
                      .clip(RoundedCornerShape(12.dp))
                      .background(barColorSelector(value))
                )
              }

              // Day of week label below bar (>= 22sp); highlight today clearly with a circle badge
              Box(
                modifier =
                  Modifier.size(38.dp)
                    .clip(CircleShape)
                    .background(if (day.isToday) accentColor else Color.Transparent),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = day.shortDayLabel,
                  fontSize = 22.sp,
                  lineHeight = 28.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (day.isToday) Color.White else InkPrimary,
                  textAlign = TextAlign.Center,
                )
              }
            }
          }
        }
      }

      // Legend showing highlighted circle = 오늘
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(accentColor))
        Text(
          text = "= ${stringResource(id = R.string.stats_today_badge)}",
          fontSize = 22.sp,
          lineHeight = 28.sp,
          fontWeight = FontWeight.Medium,
          color = InkSecondary,
        )
      }
    }
  }
}
