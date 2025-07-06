package com.charliesbot.shared.core.utils

import com.charliesbot.shared.core.constants.PredefinedFastingGoals

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
        startTimeMillis: Long,
        fastingGoalId: String,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): FastingProgress {
        val goal = PredefinedFastingGoals.getGoalById(fastingGoalId)
        val elapsedTimeMillis = (currentTimeMillis - startTimeMillis).coerceAtLeast(0L)
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