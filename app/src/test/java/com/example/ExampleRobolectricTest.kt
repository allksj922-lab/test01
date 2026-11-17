package com.example

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.DailyHealthRecord
import com.example.data.DailyHealthRepository
import com.example.data.MedicationSlot
import com.example.notifications.MedReminderConfig
import com.example.notifications.MedicationReminderReceiver
import com.example.notifications.ReminderPreferences
import com.example.notifications.ReminderSlotId
import com.example.notifications.WaterReminderNotifier
import com.example.notifications.WaterReminderReceiver
import com.example.notifications.WaterReminderScheduler
import com.example.ui.DailyCheckUiState
import com.example.ui.DailyCheckViewModel
import com.example.ui.DateSnapshot
import com.example.ui.screen.SeniorAppShell
import com.example.ui.screen.SeniorCheckScreen
import com.example.ui.theme.SeniorCareTheme
import java.util.Calendar
import org.robolectric.Shadows
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("약·물 체크", appName)
  }

  @Test
  fun `medication and water intake persist for today in Room`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db =
      Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    try {
      val repo = DailyHealthRepository(db.dailyHealthDao())
      val today = "2026-11-17"

      repo.toggleMedication(today, MedicationSlot.MORNING)
      repo.toggleMedication(today, MedicationSlot.DINNER)
      repo.incrementWater(today)
      repo.incrementWater(today)

      val persisted = repo.observeRecord(today).first()
      assertTrue(persisted.morningMedTaken)
      assertFalse(persisted.lunchMedTaken)
      assertTrue(persisted.dinnerMedTaken)
      assertEquals(2, persisted.waterCups)
    } finally {
      db.close()
    }
  }

  @Test
  fun `day rollover keeps last 7 days for stats, prunes older records, and starts new day fresh`() =
    runTest {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val db =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
          .allowMainThreadQueries()
          .build()
      try {
        val dao = db.dailyHealthDao()
        val repo = DailyHealthRepository(dao)
        val tooOldDay = "2026-11-10"
        val keptYesterday = "2026-11-17"
        val newToday = "2026-11-18"
        val oldestKeptForNewToday = "2026-11-12"

        dao.upsertRecord(
          DailyHealthRecord(dateKey = tooOldDay, morningMedTaken = true, waterCups = 4)
        )
        dao.upsertRecord(
          DailyHealthRecord(dateKey = keptYesterday, morningMedTaken = true, waterCups = 6)
        )

        repo.pruneOlderRecords(oldestKeptForNewToday)
        assertNull(dao.getRecordForDate(tooOldDay))
        assertNotNull(dao.getRecordForDate(keptYesterday))

        val newTodayRecord = repo.observeRecord(newToday).first()
        assertFalse(newTodayRecord.morningMedTaken)
        assertFalse(newTodayRecord.lunchMedTaken)
        assertFalse(newTodayRecord.dinnerMedTaken)
        assertEquals(0, newTodayRecord.waterCups)

        val rangeRecords = repo.observeRecordsInRange(oldestKeptForNewToday, newToday).first()
        assertEquals(1, rangeRecords.size)
        assertEquals(6, rangeRecords.first().waterCups)
      } finally {
        db.close()
      }
    }

  @Test
  fun `senior check screen displays date, med buttons, water counter, and clear button`() {
    composeTestRule.setContent {
      SeniorCareTheme {
        SeniorCheckScreen(
          uiState =
            DailyCheckUiState(
              dateKey = "2026-11-17",
              formattedDate = "2026년 11월 17일",
              dayOfWeekLabel = "화요일",
            ),
          onToggleMedication = {},
          onAddWaterCup = {},
          onRemoveWaterCup = {},
          onClearTodayRecord = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("2026년 11월 17일").assertIsDisplayed()
    composeTestRule.onNodeWithTag("med_button_morning").assertIsDisplayed()
    composeTestRule.onNodeWithTag("med_button_lunch").assertIsDisplayed()
    composeTestRule.onNodeWithTag("med_button_dinner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("water_count_text").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("add_water_button").performScrollTo().assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("clear_today_button")
      .performScrollTo()
      .assertIsDisplayed()
      .assertIsNotEnabled()
  }

  @Test
  fun `tapping morning med, water button, and clearing today record updates state`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db =
      Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    try {
      val repo = DailyHealthRepository(db.dailyHealthDao())
      val viewModel =
        DailyCheckViewModel(
          repository = repo,
          dateProvider = {
            DateSnapshot(
              dateKey = "2026-11-17",
              formattedDate = "2026년 11월 17일",
              dayOfWeekLabel = "화요일",
              shortDayLabel = "화",
              shortMonthDay = "11월 17일",
            )
          },
        )

      composeTestRule.setContent {
        SeniorCareTheme {
          val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
          SeniorCheckScreen(
            uiState = uiState,
            onToggleMedication = viewModel::onToggleMedication,
            onAddWaterCup = viewModel::onAddWaterCup,
            onRemoveWaterCup = viewModel::onRemoveWaterCup,
            onClearTodayRecord = viewModel::onClearTodayRecord,
          )
        }
      }

      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("med_button_morning").performClick()
      composeTestRule.onNodeWithTag("add_water_button").performScrollTo().performClick()

      composeTestRule.waitUntil(timeoutMillis = 3_000) {
        composeTestRule
          .onAllNodesWithText("복용 완료", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty() &&
          composeTestRule
            .onAllNodesWithText("1 잔", useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      }

      composeTestRule.onNodeWithText("복용 완료", useUnmergedTree = true).assertIsDisplayed()
      composeTestRule
        .onNodeWithText("1 잔", useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()

      // Tap '오늘 기록 지우기' -> confirm in dialog -> state resets to 0 and unchecked
      composeTestRule
        .onNodeWithTag("clear_today_button")
        .performScrollTo()
        .assertIsEnabled()
        .performClick()
      composeTestRule.waitForIdle()

      composeTestRule.onNodeWithTag("confirm_clear_button").performClick()

      composeTestRule.waitUntil(timeoutMillis = 3_000) {
        composeTestRule
          .onAllNodesWithText("복용 완료", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isEmpty() &&
          composeTestRule
            .onAllNodesWithText("0 잔", useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      }

      composeTestRule
        .onNodeWithText("0 잔", useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
      composeTestRule.onNodeWithTag("clear_today_button").performScrollTo().assertIsNotEnabled()
    } finally {
      db.close()
    }
  }

  @Test
  fun `navigating to weekly stats tab shows 7-day med and water bar charts`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db =
      Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    try {
      val dao = db.dailyHealthDao()
      kotlinx.coroutines.runBlocking {
        dao.upsertRecord(
          DailyHealthRecord(
            dateKey = "2026-11-15",
            morningMedTaken = true,
            lunchMedTaken = true,
            dinnerMedTaken = true,
            waterCups = 8,
          )
        )
        dao.upsertRecord(
          DailyHealthRecord(dateKey = "2026-11-17", morningMedTaken = true, waterCups = 5)
        )
      }

      val repo = DailyHealthRepository(dao)
      val viewModel =
        DailyCheckViewModel(
          repository = repo,
          dateProvider = {
            DateSnapshot(
              dateKey = "2026-11-17",
              formattedDate = "2026년 11월 17일",
              dayOfWeekLabel = "화요일",
              shortDayLabel = "화",
              shortMonthDay = "11월 17일",
            )
          },
        )

      composeTestRule.setContent {
        SeniorCareTheme {
          val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
          SeniorAppShell(
            uiState = uiState,
            onToggleMedication = viewModel::onToggleMedication,
            onAddWaterCup = viewModel::onAddWaterCup,
            onRemoveWaterCup = viewModel::onRemoveWaterCup,
            onClearTodayRecord = viewModel::onClearTodayRecord,
          )
        }
      }

      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("nav_tab_weekly_stats").performClick()

      composeTestRule.waitUntil(timeoutMillis = 3_000) {
        composeTestRule
          .onAllNodesWithText("일주일 약 복용 4 / 21회", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
      }

      composeTestRule.onNodeWithTag("weekly_summary_card").performScrollTo().assertIsDisplayed()
      composeTestRule.onNodeWithText("일주일 약 복용 4 / 21회").performScrollTo().assertIsDisplayed()
      composeTestRule
        .onNodeWithText("일주일 물 총 13잔 (하루 평균 1.9잔)")
        .performScrollTo()
        .assertIsDisplayed()
      composeTestRule.onNodeWithTag("weekly_med_chart").performScrollTo().assertIsDisplayed()
      composeTestRule.onNodeWithTag("weekly_water_chart").performScrollTo().assertIsDisplayed()
    } finally {
      db.close()
    }
  }

  @Test
  fun `notification channel and receiver post sound, vibration, and MainActivity PendingIntent`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

    WaterReminderNotifier.ensureNotificationChannel(app)
    val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = manager.getNotificationChannel(WaterReminderNotifier.CHANNEL_ID)
    assertNotNull(channel)
    assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
    assertTrue(channel.shouldVibrate())
    assertNotNull(channel.sound)

    val receiver = WaterReminderReceiver()
    receiver.onReceive(
      app,
      Intent(app, WaterReminderReceiver::class.java).apply {
        putExtra(WaterReminderScheduler.EXTRA_SLOT_ID, ReminderSlotId.MORNING.name)
      },
    )

    val shadowNm = Shadows.shadowOf(manager)
    val posted = shadowNm.allNotifications
    assertTrue(posted.isNotEmpty())
    val latest = posted.last()
    assertNotNull(latest.contentIntent)
    val shadowPendingIntent = Shadows.shadowOf(latest.contentIntent)
    assertEquals(MainActivity::class.java.name, shadowPendingIntent.savedIntent.component?.className)
  }

  @Test
  fun `reminder scheduler computes future time today or next day when time has passed`() {
    val now =
      Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 17, 14, 0, 0)
        set(Calendar.MILLISECOND, 0)
      }
    val laterToday = WaterReminderScheduler.computeNextTriggerMillis(19, 0, now.timeInMillis)
    val laterCal = Calendar.getInstance().apply { timeInMillis = laterToday }
    assertEquals(17, laterCal.get(Calendar.DAY_OF_MONTH))
    assertEquals(19, laterCal.get(Calendar.HOUR_OF_DAY))

    val tomorrowMorning = WaterReminderScheduler.computeNextTriggerMillis(9, 30, now.timeInMillis)
    val tomorrowCal = Calendar.getInstance().apply { timeInMillis = tomorrowMorning }
    assertEquals(18, tomorrowCal.get(Calendar.DAY_OF_MONTH))
    assertEquals(9, tomorrowCal.get(Calendar.HOUR_OF_DAY))
    assertEquals(30, tomorrowCal.get(Calendar.MINUTE))
  }

  @Test
  fun `navigating to reminders tab allows toggling reminder and editing time`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    val db =
      Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).allowMainThreadQueries().build()
    try {
      val repo = DailyHealthRepository(db.dailyHealthDao())
      val viewModel =
        DailyCheckViewModel(
          repository = repo,
          dateProvider = {
            DateSnapshot(
              dateKey = "2026-11-17",
              formattedDate = "2026년 11월 17일",
              dayOfWeekLabel = "화요일",
              shortDayLabel = "화",
              shortMonthDay = "11월 17일",
            )
          },
        )

      composeTestRule.setContent {
        SeniorCareTheme {
          val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
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
          )
        }
      }

      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("nav_tab_reminders").performClick()

      composeTestRule.waitUntil(timeoutMillis = 3_000) {
        composeTestRule
          .onAllNodesWithText("매일 약 복용 알림", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
      }

      composeTestRule.onNodeWithTag("reminder_toggle_morning").performScrollTo().performClick()
      composeTestRule.waitUntil(timeoutMillis = 3_000) {
        composeTestRule
          .onAllNodesWithText("알림 1개 켜짐", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
      }
      composeTestRule.onNodeWithTag("reminder_header_card").performScrollTo()
      composeTestRule.onNodeWithText("알림 1개 켜짐").assertIsDisplayed()

      composeTestRule.onNodeWithTag("reminder_time_button_morning").performScrollTo().performClick()
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("time_picker_hour_plus").performClick()
      composeTestRule.onNodeWithTag("time_picker_save_button").performClick()
      composeTestRule.waitForIdle()

      composeTestRule.onNodeWithText("오전 10시 30분").performScrollTo().assertIsDisplayed()
      composeTestRule.onNodeWithTag("test_notification_button").performScrollTo().performClick()
      composeTestRule
        .onNodeWithTag("test_notification_feedback")
        .performScrollTo()
        .assertIsDisplayed()
    } finally {
      db.close()
    }
  }

  @Test
  fun `medication reminder scheduler and receiver schedule alarm and post notification`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

    WaterReminderNotifier.ensureNotificationChannel(app)
    val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val medChannel = nm.getNotificationChannel(WaterReminderNotifier.MED_CHANNEL_ID)
    assertNotNull(medChannel)
    assertEquals(NotificationManager.IMPORTANCE_HIGH, medChannel.importance)
    assertTrue(medChannel.shouldVibrate())
    assertNotNull(medChannel.sound)

    val prefs = ReminderPreferences(app)
    val lunchConfig = MedReminderConfig(MedicationSlot.LUNCH, hour = 12, minute = 30, enabled = true)
    prefs.saveMedSlot(lunchConfig)
    WaterReminderScheduler.syncMedSlotSchedule(app, lunchConfig)

    val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val shadowAlarm = Shadows.shadowOf(alarmManager)
    assertTrue(shadowAlarm.scheduledAlarms.isNotEmpty())

    // Trigger receiver and verify posted notification + MainActivity intent
    MedicationReminderReceiver().onReceive(
      app,
      Intent(app, MedicationReminderReceiver::class.java).apply {
        putExtra(WaterReminderScheduler.EXTRA_MED_SLOT, MedicationSlot.LUNCH.name)
      },
    )

    val posted = Shadows.shadowOf(nm).allNotifications
    assertTrue(posted.isNotEmpty())
    val latest = posted.last()
    assertEquals("점심 약 드실 시간이에요", latest.extras.getString(android.app.Notification.EXTRA_TITLE))
    assertNotNull(latest.contentIntent)
    assertEquals(
      MainActivity::class.java.name,
      Shadows.shadowOf(latest.contentIntent).savedIntent.component?.className,
    )

    // Disabling cancels alarm
    WaterReminderScheduler.syncMedSlotSchedule(app, lunchConfig.copy(enabled = false))
  }

  @Test
  fun `toggling morning lunch dinner medication reminders and changing time updates UI`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    val db =
      Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).allowMainThreadQueries().build()
    try {
      val repo = DailyHealthRepository(db.dailyHealthDao())
      val viewModel =
        DailyCheckViewModel(
          repository = repo,
          dateProvider = {
            DateSnapshot(
              dateKey = "2026-11-17",
              formattedDate = "2026년 11월 17일",
              dayOfWeekLabel = "화요일",
              shortDayLabel = "화",
              shortMonthDay = "11월 17일",
            )
          },
        )

      composeTestRule.setContent {
        SeniorCareTheme {
          val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
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
          )
        }
      }

      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("nav_tab_reminders").performClick()

      composeTestRule.waitUntil(timeoutMillis = 3_000) {
        composeTestRule
          .onAllNodesWithText("아침 약 알림", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
      }

      composeTestRule.onNodeWithTag("med_reminder_toggle_morning").performScrollTo().performClick()
      composeTestRule.onNodeWithTag("med_reminder_toggle_lunch").performScrollTo().performClick()
      composeTestRule.onNodeWithTag("med_reminder_toggle_dinner").performScrollTo().performClick()

      composeTestRule.waitUntil(timeoutMillis = 3_000) {
        composeTestRule
          .onAllNodesWithText("알림 3개 켜짐", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
      }

      composeTestRule.onNodeWithTag("med_reminder_time_button_morning").performScrollTo().performClick()
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("time_picker_hour_plus").performClick()
      composeTestRule.onNodeWithTag("time_picker_save_button").performClick()
      composeTestRule.waitForIdle()

      composeTestRule.onNodeWithText("오전 9시 정각").performScrollTo().assertIsDisplayed()
      composeTestRule.onNodeWithTag("test_med_notification_button").performScrollTo().performClick()
      composeTestRule
        .onNodeWithTag("test_med_notification_feedback")
        .performScrollTo()
        .assertIsDisplayed()
    } finally {
      db.close()
    }
  }

  @Test
  fun `month records query filters by prefix and ViewModel preserves older records across months`() =
    runTest {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val db =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
          .allowMainThreadQueries()
          .build()
      try {
        val dao = db.dailyHealthDao()
        val repo = DailyHealthRepository(dao)

        dao.upsertRecord(
          DailyHealthRecord(dateKey = "2026-10-15", morningMedTaken = true, waterCups = 4)
        )
        dao.upsertRecord(
          DailyHealthRecord(dateKey = "2026-11-10", lunchMedTaken = true, waterCups = 2)
        )
        dao.upsertRecord(
          DailyHealthRecord(dateKey = "2026-11-17", dinnerMedTaken = true, waterCups = 5)
        )

        val octRecords = repo.observeMonthRecords("2026-10").first()
        assertEquals(1, octRecords.size)
        assertEquals("2026-10-15", octRecords.first().dateKey)

        val novRecords = repo.observeMonthRecords("2026-11").first()
        assertEquals(2, novRecords.size)

        // Creating ViewModel should NOT prune older months so users can browse past records
        DailyCheckViewModel(
          repository = repo,
          dateProvider = {
            DateSnapshot(
              dateKey = "2026-11-17",
              formattedDate = "2026년 11월 17일",
              dayOfWeekLabel = "화요일",
              shortDayLabel = "화",
              shortMonthDay = "11월 17일",
            )
          },
        )
        assertNotNull(dao.getRecordForDate("2026-10-15"))
      } finally {
        db.close()
      }
    }

  @Test
  fun `past history calendar screen allows navigating months and editing past medication and water`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db =
      Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    try {
      val repo = DailyHealthRepository(db.dailyHealthDao())
      val viewModel =
        DailyCheckViewModel(
          repository = repo,
          dateProvider = {
            DateSnapshot(
              dateKey = "2026-11-17",
              formattedDate = "2026년 11월 17일",
              dayOfWeekLabel = "화요일",
              shortDayLabel = "화",
              shortMonthDay = "11월 17일",
            )
          },
        )

      composeTestRule.setContent {
        SeniorCareTheme {
          val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
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

      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("nav_tab_weekly_stats").performClick()
      composeTestRule.waitForIdle()

      composeTestRule.onNodeWithTag("open_calendar_history_button").performScrollTo().performClick()
      composeTestRule.waitForIdle()

      composeTestRule.onNodeWithTag("calendar_history_screen").assertIsDisplayed()
      composeTestRule.onNodeWithTag("calendar_month_label").assertIsDisplayed()
      composeTestRule.onNodeWithText("2026년 11월").assertIsDisplayed()
      composeTestRule.onNodeWithTag("calendar_next_month").assertIsNotEnabled()

      // Select past date 2026-11-10 and edit morning medication & water
      composeTestRule.onNodeWithTag("calendar_day_2026-11-10").performScrollTo().performClick()
      composeTestRule.waitUntil(timeoutMillis = 3_000) {
        composeTestRule
          .onAllNodesWithText("2026년 11월 10일 화요일", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
      }
      composeTestRule.onNodeWithText("2026년 11월 10일 화요일").performScrollTo().assertIsDisplayed()

      composeTestRule.onNodeWithTag("calendar_med_morning").performScrollTo().performClick()
      composeTestRule.onNodeWithTag("calendar_water_plus_2026-11-10").performScrollTo().performClick()

      composeTestRule.waitUntil(timeoutMillis = 3_000) {
        composeTestRule
          .onAllNodesWithText("복용 완료", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty() &&
          composeTestRule
            .onAllNodesWithText("물 마시기: 1 잔", useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      }

      val savedNov10 = kotlinx.coroutines.runBlocking { repo.observeRecord("2026-11-10").first() }
      assertTrue(savedNov10.morningMedTaken)
      assertEquals(1, savedNov10.waterCups)

      // Navigate to previous month (2026-10) and edit 2026-10-15 dinner medication
      composeTestRule.onNodeWithTag("calendar_prev_month").performScrollTo().performClick()
      composeTestRule.waitUntil(timeoutMillis = 3_000) {
        composeTestRule
          .onAllNodesWithText("2026년 10월", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
      }
      composeTestRule.onNodeWithTag("calendar_next_month").assertIsEnabled()

      composeTestRule.onNodeWithTag("calendar_day_2026-10-15").performScrollTo().performClick()
      composeTestRule.waitUntil(timeoutMillis = 3_000) {
        composeTestRule
          .onAllNodesWithText("2026년 10월 15일 목요일", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
      }
      composeTestRule.onNodeWithTag("calendar_med_dinner").performScrollTo().performClick()

      composeTestRule.waitUntil(timeoutMillis = 3_000) {
        composeTestRule
          .onAllNodesWithText("복용 완료", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
      }
      val savedOct15 = kotlinx.coroutines.runBlocking { repo.observeRecord("2026-10-15").first() }
      assertTrue(savedOct15.dinnerMedTaken)

      // Back button returns to weekly stats screen
      composeTestRule.onNodeWithTag("calendar_back_button").performScrollTo().performClick()
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("weekly_stats_screen").assertIsDisplayed()
    } finally {
      db.close()
    }
  }
}

