package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.DailyHealthRepository
import com.example.notifications.AndroidWaterReminderGateway
import com.example.notifications.WaterReminderNotifier
import com.example.ui.DailyCheckViewModel
import com.example.ui.screen.SeniorAppShell
import com.example.ui.theme.IvoryBackground
import com.example.ui.theme.SeniorCareTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.light(IvoryBackground.toArgb(), IvoryBackground.toArgb()),
      navigationBarStyle =
        SystemBarStyle.light(IvoryBackground.toArgb(), IvoryBackground.toArgb()),
    )

    WaterReminderNotifier.ensureNotificationChannel(applicationContext)

    val database = AppDatabase.getInstance(applicationContext)
    val repository = DailyHealthRepository(database.dailyHealthDao())
    val reminderGateway = AndroidWaterReminderGateway(applicationContext)

    setContent {
      SeniorCareTheme {
        val viewModel: DailyCheckViewModel =
          viewModel(factory = DailyCheckViewModel.provideFactory(repository, reminderGateway))
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, this) {
          val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
              viewModel.refreshDateIfNeeded()
            }
          }
          lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

          val dateChangeReceiver =
            object : BroadcastReceiver() {
              override fun onReceive(context: Context?, intent: Intent?) {
                viewModel.refreshDateIfNeeded()
              }
            }
          val intentFilter =
            IntentFilter().apply {
              addAction(Intent.ACTION_DATE_CHANGED)
              addAction(Intent.ACTION_TIME_CHANGED)
              addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
          ContextCompat.registerReceiver(
            this@MainActivity,
            dateChangeReceiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
          )

          onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            unregisterReceiver(dateChangeReceiver)
          }
        }

        SeniorAppShell(
          uiState = uiState,
          onToggleMedication = viewModel::onToggleMedication,
          onAddWaterCup = viewModel::onAddWaterCup,
          onRemoveWaterCup = viewModel::onRemoveWaterCup,
          onClearTodayRecord = viewModel::onClearTodayRecord,
          onToggleMedReminder = viewModel::onToggleMedReminder,
          onUpdateMedReminderTime = viewModel::onUpdateMedReminderTime,
          onSendTestMedReminder = viewModel::onSendTestMedReminder,
          onToggleReminder = viewModel::onToggleReminder,
          onUpdateReminderTime = viewModel::onUpdateReminderTime,
          onSendTestReminder = viewModel::onSendTestReminder,
          onSelectCalendarDate = viewModel::onSelectCalendarDate,
          onPreviousCalendarMonth = viewModel::onPreviousCalendarMonth,
          onNextCalendarMonth = viewModel::onNextCalendarMonth,
          onToggleMedicationForDate = viewModel::onToggleMedicationForDate,
          onIncrementWaterForDate = viewModel::onIncrementWaterForDate,
          onDecrementWaterForDate = viewModel::onDecrementWaterForDate,
        )
      }
    }
  }
}
