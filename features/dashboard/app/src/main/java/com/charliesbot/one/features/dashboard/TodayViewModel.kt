package com.charliesbot.one.features.dashboard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.constants.FastGoal
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.domain.repository.CustomGoalRepository
import com.charliesbot.shared.core.domain.repository.FastingHistoryRepository
import com.charliesbot.shared.core.domain.repository.SettingsRepository
import com.charliesbot.shared.core.domain.usecase.GetSuggestedFastingStartTimeUseCase
import com.charliesbot.shared.core.domain.usecase.ObserveFastingStateUseCase
import com.charliesbot.shared.core.domain.usecase.StartFastingUseCase
import com.charliesbot.shared.core.domain.usecase.StopFastingUseCase
import com.charliesbot.shared.core.domain.usecase.UpdateFastingConfigUseCase
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.models.SuggestedFastingTime
import com.charliesbot.shared.core.models.TimePeriodProgress
import com.charliesbot.shared.core.utils.FastingProgressCalculator
import com.charliesbot.shared.core.utils.GoalResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodayViewModel(
    application: Application,
    fastingHistoryRepository: FastingHistoryRepository,
    private val observeFastingStateUseCase: ObserveFastingStateUseCase,
    private val startFastingUseCase: StartFastingUseCase,
    private val stopFastingUseCase: StopFastingUseCase,
    private val updateFastingConfigUseCase: UpdateFastingConfigUseCase,
    private val settingsRepository: SettingsRepository,
    private val getSuggestedFastingStartTimeUseCase: GetSuggestedFastingStartTimeUseCase,
    private val customGoalRepository: CustomGoalRepository,
    goalResolver: GoalResolver,
) : AndroidViewModel(application) {
    private val currentFasting: StateFlow<FastingDataItem?> = observeFastingStateUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null,
        )
    val isFasting: StateFlow<Boolean> = currentFasting
        .map { it?.isFasting ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)
    val startTimeInMillis: StateFlow<Long> = currentFasting
        .map { it?.startTimeInMillis ?: -1L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), -1L)
    val fastingGoalId: StateFlow<String> = currentFasting
        .map { it?.fastingGoalId ?: PredefinedFastingGoals.SIXTEEN_EIGHT.id }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            PredefinedFastingGoals.SIXTEEN_EIGHT.id,
        )
    val weeklyProgress: StateFlow<List<TimePeriodProgress>> =
        fastingHistoryRepository.getCurrentWeekHistory()
            .map { records -> FastingProgressCalculator.calculateWeeklyProgress(records) }
            .stateIn(
                scope = viewModelScope,
                SharingStarted.WhileSubscribed(5000L),
                emptyList(),
            )

    val allGoals: StateFlow<List<FastGoal>> = goalResolver.allGoals
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            PredefinedFastingGoals.allGoals,
        )

    // Smart Reminders
    val smartRemindersEnabled: StateFlow<Boolean> = settingsRepository.smartRemindersEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    private val _suggestedFastingTime = MutableStateFlow<SuggestedFastingTime?>(null)
    val suggestedFastingTime: StateFlow<SuggestedFastingTime?> = _suggestedFastingTime

    init {
        // Load suggested time when view model is created
        loadSuggestedFastingTime()

        // Reload when bedtime changes (skip initial emission to avoid duplicate load)
        viewModelScope.launch {
            settingsRepository.bedtimeMinutes.drop(1).collect { _ ->
                Log.d(LOG_TAG, "TodayViewModel: Bedtime changed, reloading suggestion")
                loadSuggestedFastingTime()
            }
        }
    }

    /**
     * Reload the suggested fasting time. Called on init and when settings change.
     */
    fun refreshSuggestedTime() {
        loadSuggestedFastingTime()
    }

    private fun loadSuggestedFastingTime() {
        viewModelScope.launch {
            try {
                val suggestion = getSuggestedFastingStartTimeUseCase.execute()
                _suggestedFastingTime.value = suggestion
                Log.d(
                    LOG_TAG,
                    "TodayViewModel: Loaded suggestion - ${suggestion.suggestedTimeMinutes}min, ${suggestion.reasoning}",
                )
            } catch (e: Exception) {
                Log.e(LOG_TAG, "TodayViewModel: Failed to load suggested time", e)
            }
        }
    }

    private val _isTimePickerDialogOpen = MutableStateFlow(false)
    val isTimePickerDialogOpen: StateFlow<Boolean> = _isTimePickerDialogOpen

    private val _isGoalBottomSheetOpen = MutableStateFlow(false)
    val isGoalBottomSheetOpen: StateFlow<Boolean> = _isGoalBottomSheetOpen

    fun openTimePickerDialog() {
        _isTimePickerDialogOpen.value = true
    }

    fun closeTimePickerDialog() {
        _isTimePickerDialogOpen.value = false
    }

    fun openGoalBottomSheet() {
        _isGoalBottomSheetOpen.value = true
    }

    fun closeGoalBottomSheet() {
        _isGoalBottomSheetOpen.value = false
    }

    fun onStopFasting() {
        viewModelScope.launch {
            stopFastingUseCase()
        }
    }

    fun onStartFasting() {
        viewModelScope.launch {
            startFastingUseCase(fastingGoalId.value)
        }
    }

    fun updateStartTime(timeInMillis: Long) {
        viewModelScope.launch {
            updateFastingConfigUseCase(startTimeMillis = timeInMillis)
        }
    }

    fun updateFastingGoal(fastingGoalId: String) {
        viewModelScope.launch {
            updateFastingConfigUseCase(goalId = fastingGoalId)
        }
    }

    fun saveCustomGoal(goal: FastGoal) {
        viewModelScope.launch {
            customGoalRepository.saveCustomGoal(goal)
            updateFastingConfigUseCase(goalId = goal.id)
        }
    }

    fun deleteCustomGoal(goalId: String) {
        viewModelScope.launch {
            customGoalRepository.deleteCustomGoal(goalId)
            // If the deleted goal was active, fall back to 16:8
            if (fastingGoalId.value == goalId) {
                updateFastingConfigUseCase(goalId = PredefinedFastingGoals.SIXTEEN_EIGHT.id)
            }
        }
    }
}
