package com.charliesbot.one.features.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.charliesbot.shared.core.components.FastingDayData
import com.charliesbot.shared.core.domain.usecase.GetMonthlyFastingMapUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
    val fastingData: Map<LocalDate, FastingDayData> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class)
class YouViewModel(
    application: Application,
    getMonthlyFastingMapUseCase: GetMonthlyFastingMapUseCase
) : AndroidViewModel(application) {
    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth

    private val fastingDataFlow: Flow<Map<LocalDate, FastingDayData>> =
        _selectedMonth.flatMapLatest { month ->
            getMonthlyFastingMapUseCase(month)
        }

    val uiState: StateFlow<CalendarUiState> = combine(
        _selectedMonth,
        fastingDataFlow
    ) { month, data ->
        CalendarUiState(
            selectedMonth = month,
            fastingData = data
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

}