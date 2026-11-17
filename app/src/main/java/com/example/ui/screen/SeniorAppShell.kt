package com.example.ui.screen

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.MedicationSlot
import com.example.notifications.ReminderSlotId
import com.example.ui.DailyCheckUiState
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.MedCompletedGreenDark
import com.example.ui.theme.MedCompletedGreenSoft
import com.example.ui.theme.WarmCardSurface
import com.example.ui.theme.WarmHeaderSurface

const val ROUTE_TODAY = "today"
const val ROUTE_WEEKLY_STATS = "weekly_stats"
const val ROUTE_REMINDERS = "reminders"
const val ROUTE_CALENDAR_HISTORY = "calendar_history"

private data class TopDestination(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private val TopDestinations =
  listOf(
    TopDestination(
      route = ROUTE_TODAY,
      labelRes = R.string.nav_today,
      selectedIcon = Icons.Filled.CheckCircle,
      unselectedIcon = Icons.Outlined.CheckCircle,
      testTag = "nav_tab_today",
    ),
    TopDestination(
      route = ROUTE_WEEKLY_STATS,
      labelRes = R.string.nav_weekly_stats,
      selectedIcon = Icons.Filled.BarChart,
      unselectedIcon = Icons.Outlined.BarChart,
      testTag = "nav_tab_weekly_stats",
    ),
    TopDestination(
      route = ROUTE_REMINDERS,
      labelRes = R.string.nav_reminders,
      selectedIcon = Icons.Filled.NotificationsActive,
      unselectedIcon = Icons.Outlined.Notifications,
      testTag = "nav_tab_reminders",
    ),
  )

@Composable
fun SeniorAppShell(
  uiState: DailyCheckUiState,
  onToggleMedication: (MedicationSlot) -> Unit,
  onAddWaterCup: () -> Unit,
  onRemoveWaterCup: () -> Unit,
  onClearTodayRecord: () -> Unit,
  onToggleMedReminder: (MedicationSlot, Boolean) -> Unit = { _, _ -> },
  onUpdateMedReminderTime: (MedicationSlot, Int, Int) -> Unit = { _, _, _ -> },
  onSendTestMedReminder: () -> Unit = {},
  onToggleReminder: (ReminderSlotId, Boolean) -> Unit = { _, _ -> },
  onUpdateReminderTime: (ReminderSlotId, Int, Int) -> Unit = { _, _, _ -> },
  onSendTestReminder: () -> Unit = {},
  onSelectCalendarDate: (String) -> Unit = {},
  onPreviousCalendarMonth: () -> Unit = {},
  onNextCalendarMonth: () -> Unit = {},
  onToggleMedicationForDate: (String, MedicationSlot) -> Unit = { _, _ -> },
  onIncrementWaterForDate: (String) -> Unit = {},
  onDecrementWaterForDate: (String) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: ROUTE_TODAY
  val isTopLevelDestination = currentRoute != ROUTE_CALENDAR_HISTORY

  val navigateToTab: (String) -> Unit = { route ->
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = MaterialTheme.colorScheme.background,
      contentWindowInsets = WindowInsets.safeDrawing,
      bottomBar = {
        if (!useNavigationRail && isTopLevelDestination) {
          NavigationBar(
            containerColor = WarmCardSurface,
            tonalElevation = 4.dp,
            modifier = Modifier.heightIn(min = 84.dp),
          ) {
            TopDestinations.forEach { dest ->
              val selected = currentRoute == dest.route
              NavigationBarItem(
                selected = selected,
                onClick = { navigateToTab(dest.route) },
                modifier = Modifier.testTag(dest.testTag),
                icon = {
                  Icon(
                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                  )
                },
                label = {
                  Text(
                    text = stringResource(id = dest.labelRes),
                    fontSize = 22.sp,
                    lineHeight = 26.sp,
                    softWrap = false,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  )
                },
                colors =
                  NavigationBarItemDefaults.colors(
                    selectedIconColor = MedCompletedGreenDark,
                    selectedTextColor = MedCompletedGreenDark,
                    indicatorColor = MedCompletedGreenSoft,
                    unselectedIconColor = InkSecondary,
                    unselectedTextColor = InkPrimary,
                  ),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize()) {
        if (useNavigationRail && isTopLevelDestination) {
          NavigationRail(
            containerColor = WarmHeaderSurface,
            modifier = Modifier.widthIn(min = 128.dp).padding(vertical = 12.dp),
          ) {
            TopDestinations.forEach { dest ->
              val selected = currentRoute == dest.route
              NavigationRailItem(
                selected = selected,
                onClick = { navigateToTab(dest.route) },
                modifier = Modifier.padding(vertical = 8.dp).testTag(dest.testTag),
                icon = {
                  Icon(
                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                  )
                },
                label = {
                  Text(
                    text = stringResource(id = dest.labelRes),
                    fontSize = 22.sp,
                    lineHeight = 26.sp,
                    softWrap = false,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  )
                },
                colors =
                  NavigationRailItemDefaults.colors(
                    selectedIconColor = MedCompletedGreenDark,
                    selectedTextColor = MedCompletedGreenDark,
                    indicatorColor = MedCompletedGreenSoft,
                    unselectedIconColor = InkSecondary,
                    unselectedTextColor = InkPrimary,
                  ),
              )
            }
          }
        }

        val contentPadding =
          if (useNavigationRail) innerPadding
          else
            PaddingValues(
              top = innerPadding.calculateTopPadding(),
              bottom = innerPadding.calculateBottomPadding(),
            )

        NavHost(
          navController = navController,
          startDestination = ROUTE_TODAY,
          modifier = Modifier.weight(1f).fillMaxSize(),
        ) {
          composable(ROUTE_TODAY) {
            SeniorCheckScreen(
              uiState = uiState,
              onToggleMedication = onToggleMedication,
              onAddWaterCup = onAddWaterCup,
              onRemoveWaterCup = onRemoveWaterCup,
              onClearTodayRecord = onClearTodayRecord,
              contentPadding = contentPadding,
            )
          }

          composable(ROUTE_WEEKLY_STATS) {
            WeeklyStatsScreen(
              uiState = uiState,
              onOpenCalendarHistory = { navController.navigate(ROUTE_CALENDAR_HISTORY) },
              contentPadding = contentPadding,
            )
          }

          composable(ROUTE_CALENDAR_HISTORY) {
            PastHistoryCalendarScreen(
              uiState = uiState,
              onNavigateBack = { navController.popBackStack() },
              onSelectDate = onSelectCalendarDate,
              onPreviousMonth = onPreviousCalendarMonth,
              onNextMonth = onNextCalendarMonth,
              onToggleMedicationForDate = onToggleMedicationForDate,
              onIncrementWaterForDate = onIncrementWaterForDate,
              onDecrementWaterForDate = onDecrementWaterForDate,
              contentPadding = contentPadding,
            )
          }

          composable(ROUTE_REMINDERS) {
            WaterReminderScreen(
              uiState = uiState,
              onToggleMedReminder = onToggleMedReminder,
              onUpdateMedReminderTime = onUpdateMedReminderTime,
              onSendTestMedReminder = onSendTestMedReminder,
              onToggleReminder = onToggleReminder,
              onUpdateReminderTime = onUpdateReminderTime,
              onSendTestReminder = onSendTestReminder,
              contentPadding = contentPadding,
            )
          }
        }
      }
    }
  }
}
