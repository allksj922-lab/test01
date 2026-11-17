package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DailyHealthRecord
import com.example.data.DailyHealthRepository
import com.example.data.MedicationSlot
import com.example.notifications.MedReminderConfig
import com.example.notifications.NoOpWaterReminderGateway
import com.example.notifications.ReminderSlotId
import com.example.notifications.WaterReminderConfig
import com.example.notifications.WaterReminderGateway
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DateSnapshot(
  val dateKey: String,
  val formattedDate: String,
  val dayOfWeekLabel: String,
  val shortDayLabel: String = "",
  val shortMonthDay: String = "",
) {
  companion object {
    fun fromCalendar(calendar: Calendar): DateSnapshot {
      val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
      val displayFormat = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREAN)
      val weekdayFormat = SimpleDateFormat("EEEE", Locale.KOREAN)
      val shortDayFormat = SimpleDateFormat("E", Locale.KOREAN)
      val shortMonthDayFormat = SimpleDateFormat("M월 d일", Locale.KOREAN)
      return DateSnapshot(
        dateKey = keyFormat.format(calendar.time),
        formattedDate = displayFormat.format(calendar.time),
        dayOfWeekLabel = weekdayFormat.format(calendar.time),
        shortDayLabel = shortDayFormat.format(calendar.time),
        shortMonthDay = shortMonthDayFormat.format(calendar.time),
      )
    }

    fun fromDateKey(dateKey: String): DateSnapshot {
      val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
      val cal = Calendar.getInstance()
      parser.parse(dateKey)?.let { parsed -> cal.time = parsed }
      return fromCalendar(cal)
    }

    fun today(): DateSnapshot = fromCalendar(Calendar.getInstance())

    fun recentSevenDays(anchorDateKey: String? = null): List<DateSnapshot> {
      val base = Calendar.getInstance()
      if (anchorDateKey != null) {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        parser.parse(anchorDateKey)?.let { parsed -> base.time = parsed }
      }
      return (-6..0).map { offset ->
        val dayCal = (base.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
        fromCalendar(dayCal)
      }
    }
  }
}

data class CalendarMonth(val year: Int, val monthOneBased: Int) {
  val prefix: String
    get() = String.format(Locale.US, "%04d-%02d", year, monthOneBased)

  val displayLabel: String
    get() = "${year}년 ${monthOneBased}월"

  fun previousMonth(): CalendarMonth =
    if (monthOneBased == 1) CalendarMonth(year - 1, 12) else CalendarMonth(year, monthOneBased - 1)

  fun nextMonth(): CalendarMonth =
    if (monthOneBased == 12) CalendarMonth(year + 1, 1) else CalendarMonth(year, monthOneBased + 1)

  operator fun compareTo(other: CalendarMonth): Int {
    return if (year != other.year) year.compareTo(other.year)
    else monthOneBased.compareTo(other.monthOneBased)
  }

  companion object {
    fun fromDateKey(dateKey: String): CalendarMonth {
      val parts = dateKey.split("-")
      if (parts.size >= 2) {
        val y = parts[0].toIntOrNull()
        val m = parts[1].toIntOrNull()
        if (y != null && m != null) return CalendarMonth(y, m)
      }
      val cal = Calendar.getInstance()
      return CalendarMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }
  }
}

data class CalendarDayUi(
  val dateKey: String,
  val dayOfMonth: Int,
  val isToday: Boolean,
  val isSelected: Boolean,
  val isFuture: Boolean,
  val takenMedCount: Int,
  val waterCups: Int,
)

data class DailySummaryUi(
  val dateKey: String,
  val shortDayLabel: String,
  val shortMonthDay: String,
  val isToday: Boolean,
  val takenMedCount: Int,
  val waterCups: Int,
)

data class DailyCheckUiState(
  val dateKey: String = "",
  val formattedDate: String = "",
  val dayOfWeekLabel: String = "",
  val morningMedTaken: Boolean = false,
  val lunchMedTaken: Boolean = false,
  val dinnerMedTaken: Boolean = false,
  val waterCups: Int = 0,
  val dailyWaterGoal: Int = 8,
  val weeklyDays: List<DailySummaryUi> = emptyList(),
  val weeklyRangeLabel: String = "",
  val medReminders: List<MedReminderConfig> = emptyList(),
  val waterReminders: List<WaterReminderConfig> = emptyList(),
  val calendarMonthLabel: String = "",
  val canGoNextCalendarMonth: Boolean = false,
  val calendarCells: List<CalendarDayUi?> = emptyList(),
  val selectedCalendarDateKey: String = "",
  val selectedCalendarDateLabel: String = "",
  val selectedCalendarRecord: DailyHealthRecord = DailyHealthRecord(dateKey = ""),
) {
  val takenMedCount: Int
    get() =
      (if (morningMedTaken) 1 else 0) +
        (if (lunchMedTaken) 1 else 0) +
        (if (dinnerMedTaken) 1 else 0)

  val hasAnyRecord: Boolean
    get() = morningMedTaken || lunchMedTaken || dinnerMedTaken || waterCups > 0

  val weeklyMedTakenTotal: Int
    get() = weeklyDays.sumOf { it.takenMedCount }

  val weeklyWaterTotal: Int
    get() = weeklyDays.sumOf { it.waterCups }

  val weeklyWaterAverageFormatted: String
    get() {
      val avg = if (weeklyDays.isEmpty()) 0.0 else weeklyWaterTotal.toDouble() / weeklyDays.size
      return String.format(Locale.KOREAN, "%.1f", avg)
    }

  val hasAnyWeeklyRecord: Boolean
    get() = weeklyDays.any { it.takenMedCount > 0 || it.waterCups > 0 }

  val activeReminderCount: Int
    get() = medReminders.count { it.enabled } + waterReminders.count { it.enabled }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DailyCheckViewModel(
  private val repository: DailyHealthRepository,
  private val reminderGateway: WaterReminderGateway = NoOpWaterReminderGateway,
  private val dateProvider: () -> DateSnapshot = { DateSnapshot.today() },
) : ViewModel() {

  private val currentDate = MutableStateFlow(dateProvider())
  private val selectedCalendarDateKey = MutableStateFlow(currentDate.value.dateKey)
  private val displayedCalendarMonth =
    MutableStateFlow(CalendarMonth.fromDateKey(currentDate.value.dateKey))
  private val medReminders = MutableStateFlow(reminderGateway.loadMedReminders())
  private val waterReminders = MutableStateFlow(reminderGateway.loadReminders())

  private val reminderPairFlow =
    combine(medReminders, waterReminders) { medList, waterList -> medList to waterList }

  private val calendarFlow =
    combine(displayedCalendarMonth, selectedCalendarDateKey) { month, selectedKey ->
      month to selectedKey
    }
      .flatMapLatest { (month, selectedKey) ->
        combine(
          repository.observeMonthRecords(month.prefix),
          repository.observeRecord(selectedKey),
        ) { monthRecords, selectedRecord ->
          Triple(month, monthRecords, selectedRecord)
        }
      }

  val uiState: StateFlow<DailyCheckUiState> =
    currentDate
      .flatMapLatest { snapshot ->
        val sevenDays = DateSnapshot.recentSevenDays(snapshot.dateKey)
        val startDateKey = sevenDays.first().dateKey
        val endDateKey = snapshot.dateKey
        val rangeLabel = "${sevenDays.first().shortMonthDay} ~ ${sevenDays.last().shortMonthDay}"
        val todayMonth = CalendarMonth.fromDateKey(snapshot.dateKey)

        combine(
          repository.observeRecord(snapshot.dateKey),
          repository.observeRecordsInRange(startDateKey, endDateKey),
          calendarFlow,
          reminderPairFlow,
        ) {
          todayRecord: DailyHealthRecord,
          weeklyRecords: List<DailyHealthRecord>,
          calendarData: Triple<CalendarMonth, List<DailyHealthRecord>, DailyHealthRecord>,
          reminderPair: Pair<List<MedReminderConfig>, List<WaterReminderConfig>> ->
          val recordByDate = weeklyRecords.associateBy { it.dateKey }
          val weeklySummaries =
            sevenDays.map { day ->
              val rec = recordByDate[day.dateKey]
              DailySummaryUi(
                dateKey = day.dateKey,
                shortDayLabel = day.shortDayLabel,
                shortMonthDay = day.shortMonthDay,
                isToday = day.dateKey == snapshot.dateKey,
                takenMedCount = rec?.takenMedicationCount ?: 0,
                waterCups = rec?.waterCups ?: 0,
              )
            }

          val (month, monthRecords, selectedRecord) = calendarData
          val selectedSnap = DateSnapshot.fromDateKey(selectedRecord.dateKey)
          val cells =
            buildCalendarCells(
              month = month,
              todayDateKey = snapshot.dateKey,
              selectedDateKey = selectedRecord.dateKey,
              recordsByDate = monthRecords.associateBy { it.dateKey },
            )

          DailyCheckUiState(
            dateKey = snapshot.dateKey,
            formattedDate = snapshot.formattedDate,
            dayOfWeekLabel = snapshot.dayOfWeekLabel,
            morningMedTaken = todayRecord.morningMedTaken,
            lunchMedTaken = todayRecord.lunchMedTaken,
            dinnerMedTaken = todayRecord.dinnerMedTaken,
            waterCups = todayRecord.waterCups,
            dailyWaterGoal = 8,
            weeklyDays = weeklySummaries,
            weeklyRangeLabel = rangeLabel,
            medReminders = reminderPair.first,
            waterReminders = reminderPair.second,
            calendarMonthLabel = month.displayLabel,
            canGoNextCalendarMonth = month < todayMonth,
            calendarCells = cells,
            selectedCalendarDateKey = selectedRecord.dateKey,
            selectedCalendarDateLabel =
              "${selectedSnap.formattedDate} ${selectedSnap.dayOfWeekLabel}",
            selectedCalendarRecord = selectedRecord,
          )
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue =
          currentDate.value.let { snapshot ->
            val sevenDays = DateSnapshot.recentSevenDays(snapshot.dateKey)
            val initMonth = CalendarMonth.fromDateKey(snapshot.dateKey)
            DailyCheckUiState(
              dateKey = snapshot.dateKey,
              formattedDate = snapshot.formattedDate,
              dayOfWeekLabel = snapshot.dayOfWeekLabel,
              weeklyDays =
                sevenDays.map { day ->
                  DailySummaryUi(
                    dateKey = day.dateKey,
                    shortDayLabel = day.shortDayLabel,
                    shortMonthDay = day.shortMonthDay,
                    isToday = day.dateKey == snapshot.dateKey,
                    takenMedCount = 0,
                    waterCups = 0,
                  )
                },
              weeklyRangeLabel =
                "${sevenDays.first().shortMonthDay} ~ ${sevenDays.last().shortMonthDay}",
              medReminders = medReminders.value,
              waterReminders = waterReminders.value,
              calendarMonthLabel = initMonth.displayLabel,
              canGoNextCalendarMonth = false,
              calendarCells =
                buildCalendarCells(
                  month = initMonth,
                  todayDateKey = snapshot.dateKey,
                  selectedDateKey = snapshot.dateKey,
                  recordsByDate = emptyMap(),
                ),
              selectedCalendarDateKey = snapshot.dateKey,
              selectedCalendarDateLabel = "${snapshot.formattedDate} ${snapshot.dayOfWeekLabel}",
              selectedCalendarRecord = DailyHealthRecord(dateKey = snapshot.dateKey),
            )
          },
      )

  fun refreshDateIfNeeded() {
    val latest = dateProvider()
    val previousToday = currentDate.value.dateKey
    if (latest.dateKey != previousToday) {
      currentDate.value = latest
      if (selectedCalendarDateKey.value == previousToday) {
        selectedCalendarDateKey.value = latest.dateKey
      }
      displayedCalendarMonth.value = CalendarMonth.fromDateKey(latest.dateKey)
    }
  }

  fun onToggleMedication(slot: MedicationSlot) {
    val dateKey = currentDate.value.dateKey
    viewModelScope.launch { repository.toggleMedication(dateKey, slot) }
  }

  fun onAddWaterCup() {
    val dateKey = currentDate.value.dateKey
    viewModelScope.launch { repository.incrementWater(dateKey) }
  }

  fun onRemoveWaterCup() {
    val dateKey = currentDate.value.dateKey
    viewModelScope.launch { repository.decrementWater(dateKey) }
  }

  fun onClearTodayRecord() {
    val dateKey = currentDate.value.dateKey
    viewModelScope.launch { repository.clearRecord(dateKey) }
  }

  fun onSelectCalendarDate(dateKey: String) {
    if (dateKey <= currentDate.value.dateKey) {
      selectedCalendarDateKey.value = dateKey
    }
  }

  fun onPreviousCalendarMonth() {
    displayedCalendarMonth.value = displayedCalendarMonth.value.previousMonth()
  }

  fun onNextCalendarMonth() {
    val todayMonth = CalendarMonth.fromDateKey(currentDate.value.dateKey)
    val next = displayedCalendarMonth.value.nextMonth()
    if (next <= todayMonth) {
      displayedCalendarMonth.value = next
    }
  }

  fun onToggleMedicationForDate(dateKey: String, slot: MedicationSlot) {
    if (dateKey <= currentDate.value.dateKey) {
      viewModelScope.launch { repository.toggleMedication(dateKey, slot) }
    }
  }

  fun onIncrementWaterForDate(dateKey: String) {
    if (dateKey <= currentDate.value.dateKey) {
      viewModelScope.launch { repository.incrementWater(dateKey) }
    }
  }

  fun onDecrementWaterForDate(dateKey: String) {
    if (dateKey <= currentDate.value.dateKey) {
      viewModelScope.launch { repository.decrementWater(dateKey) }
    }
  }

  fun onToggleMedReminder(slot: MedicationSlot, enabled: Boolean) {
    val updated =
      medReminders.value.map { config ->
        if (config.slot == slot) {
          val next = config.copy(enabled = enabled)
          reminderGateway.updateMedReminder(next)
          next
        } else {
          config
        }
      }
    medReminders.value = updated
  }

  fun onUpdateMedReminderTime(slot: MedicationSlot, hour: Int, minute: Int) {
    val updated =
      medReminders.value.map { config ->
        if (config.slot == slot) {
          val next =
            config.copy(
              hour = hour.coerceIn(0, 23),
              minute = minute.coerceIn(0, 59),
              enabled = true,
            )
          reminderGateway.updateMedReminder(next)
          next
        } else {
          config
        }
      }
    medReminders.value = updated
  }

  fun onSendTestMedReminder() {
    reminderGateway.sendTestMedNotification()
  }

  fun onToggleReminder(slotId: ReminderSlotId, enabled: Boolean) {
    val updated =
      waterReminders.value.map { config ->
        if (config.slotId == slotId) {
          val next = config.copy(enabled = enabled)
          reminderGateway.updateReminder(next)
          next
        } else {
          config
        }
      }
    waterReminders.value = updated
  }

  fun onUpdateReminderTime(slotId: ReminderSlotId, hour: Int, minute: Int) {
    val updated =
      waterReminders.value.map { config ->
        if (config.slotId == slotId) {
          val next =
            config.copy(
              hour = hour.coerceIn(0, 23),
              minute = minute.coerceIn(0, 59),
              enabled = true,
            )
          reminderGateway.updateReminder(next)
          next
        } else {
          config
        }
      }
    waterReminders.value = updated
  }

  fun onSendTestReminder() {
    reminderGateway.sendTestNotification()
  }

  private fun buildCalendarCells(
    month: CalendarMonth,
    todayDateKey: String,
    selectedDateKey: String,
    recordsByDate: Map<String, DailyHealthRecord>,
  ): List<CalendarDayUi?> {
    val cal =
      Calendar.getInstance().apply {
        set(Calendar.YEAR, month.year)
        set(Calendar.MONTH, month.monthOneBased - 1)
        set(Calendar.DAY_OF_MONTH, 1)
      }
    val leadingEmptyCount = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val cells = mutableListOf<CalendarDayUi?>()
    repeat(leadingEmptyCount) { cells.add(null) }
    for (day in 1..daysInMonth) {
      val key = String.format(Locale.US, "%04d-%02d-%02d", month.year, month.monthOneBased, day)
      val record = recordsByDate[key]
      cells.add(
        CalendarDayUi(
          dateKey = key,
          dayOfMonth = day,
          isToday = key == todayDateKey,
          isSelected = key == selectedDateKey,
          isFuture = key > todayDateKey,
          takenMedCount = record?.takenMedicationCount ?: 0,
          waterCups = record?.waterCups ?: 0,
        )
      )
    }
    return cells
  }

  companion object {
    fun provideFactory(
      repository: DailyHealthRepository,
      reminderGateway: WaterReminderGateway,
    ): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return DailyCheckViewModel(repository, reminderGateway) as T
        }
      }
  }
}
