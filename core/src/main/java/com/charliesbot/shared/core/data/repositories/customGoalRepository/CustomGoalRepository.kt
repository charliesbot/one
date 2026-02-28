package com.charliesbot.shared.core.data.repositories.customGoalRepository

import com.charliesbot.shared.core.constants.FastGoal
import kotlinx.coroutines.flow.Flow

interface CustomGoalRepository {
    val customGoals: Flow<List<FastGoal>>
    suspend fun saveCustomGoal(goal: FastGoal)
    suspend fun deleteCustomGoal(goalId: String)
}
