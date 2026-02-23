package com.charliesbot.shared.core.utils

import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.models.FastingDataItem

data class FastingProgress(
    val elapsedTimeMillis: Long,
    val elapsedHours: Long,
    val targetHours: Long,
    val progressPercentage: Int,
    val isComplete: Boolean,
    val remainingTimeMillis: Long,
    val remainingHours: Long
)

object FastingProgressUtil {
    fun calculateFastingProgress(
        fastingDataItem: FastingDataItem,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): FastingProgress {
        val goal = PredefinedFastingGoals.getGoalById(fastingDataItem.fastingGoalId)
        val elapsedTimeMillis =
            (currentTimeMillis - fastingDataItem.startTimeInMillis).coerceAtLeast(0L)
        val elapsedHours = getHours(elapsedTimeMillis)
        val targetHours = getHours(goal.durationMillis)

        val progressPercentage = calculateProgressPercentage(elapsedTimeMillis, goal.durationMillis)
        val isComplete = elapsedTimeMillis >= goal.durationMillis
        val remainingTimeMillis = (goal.durationMillis - elapsedTimeMillis).coerceAtLeast(0L)
        val remainingHours = getHours(remainingTimeMillis)

        return FastingProgress(
            elapsedTimeMillis = elapsedTimeMillis,
            elapsedHours = elapsedHours,
            targetHours = targetHours,
            progressPercentage = progressPercentage,
            isComplete = isComplete,
            remainingTimeMillis = remainingTimeMillis,
            remainingHours = remainingHours
        )
    }
}