package com.charliesbot.shared.core.domain.usecase

import com.charliesbot.shared.core.components.FastingDayData
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.data.repositories.fastingHistoryRepository.FastingHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class GetMonthlyFastingMapUseCase(
    private val repository: FastingHistoryRepository,
) {
    operator fun invoke(month: YearMonth): Flow<Map<LocalDate, FastingDayData>> {
       return repository.getFastingsForMonth(month).map { fasts->
           val fastsByDay = fasts.groupBy {
               Instant.ofEpochMilli(it.endTimeEpochMillis)
                   .atZone(ZoneId.systemDefault())
                   .toLocalDate()
           }

           fastsByDay.mapValues {(date, fastsOnDay) ->
               val longestFast = fastsOnDay.maxByOrNull { it.endTimeEpochMillis - it.startTimeEpochMillis }!!
               val durationMillis = longestFast.endTimeEpochMillis - longestFast.startTimeEpochMillis
               val durationHours = (durationMillis / 3_600_000).toInt() // ms to hours

               FastingDayData(
                   date = date,
                   durationHours = durationHours,
                   isGoalMet = durationHours >= PredefinedFastingGoals.MIN_FASTING_HOURS,
                   startTimeEpochMillis = longestFast.startTimeEpochMillis,
                   endTimeEpochMillis = longestFast.endTimeEpochMillis
               )
           }
       }
    }
}