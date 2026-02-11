package com.charliesbot.one.features.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.charliesbot.shared.core.components.FastingDayData
import com.charliesbot.shared.core.data.repositories.fastingHistoryRepository.FastingHistoryRepository
import com.charliesbot.shared.core.domain.usecase.GetMonthlyFastingMapUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
    val fastingData: Map<LocalDate, FastingDayData> = emptyMap(),
    val selectedDay: FastingDayData? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class YouViewModel(
    application: Application,
    getMonthlyFastingMapUseCase: GetMonthlyFastingMapUseCase,
    private val fastingHistoryRepository: FastingHistoryRepository
) : AndroidViewModel(application) {
    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth

    private val _selectedDay = MutableStateFlow<FastingDayData?>(null)

    private val fastingDataFlow: Flow<Map<LocalDate, FastingDayData>> =
        _selectedMonth.flatMapLatest { month ->
            getMonthlyFastingMapUseCase(month)
        }

    val uiState: StateFlow<CalendarUiState> = combine(
        _selectedMonth,
        fastingDataFlow,
        _selectedDay
    ) { month, data, selectedDay ->
        CalendarUiState(
            selectedMonth = month,
            fastingData = data,
            selectedDay = selectedDay
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    fun onNextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
    }

    fun onPreviousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun onDaySelected(day: FastingDayData?) {
        _selectedDay.value = day
    }

    fun onDeleteFastingEntry(startTimeEpochMillis: Long) {
        viewModelScope.launch {
            fastingHistoryRepository.deleteFastingRecord(startTimeEpochMillis)
            _selectedDay.value = null
        }
    }

    fun onUpdateFastingEntry(
        originalStartTime: Long,
        newStartTime: Long,
        newEndTime: Long,
        goalId: String
    ) {
        viewModelScope.launch {
            fastingHistoryRepository.updateFastingRecord(
                originalStartTime = originalStartTime,
                newStartTime = newStartTime,
                newEndTime = newEndTime,
                goalId = goalId
            )
            // Update selected day with new times so the bottom sheet reflects the change
            val durationMillis = newEndTime - newStartTime
            val newDurationHours = (durationMillis / 3_600_000).toInt()
            _selectedDay.value = _selectedDay.value?.copy(
                startTimeEpochMillis = newStartTime,
                endTimeEpochMillis = newEndTime,
                durationHours = newDurationHours
            )
        }
    }
}