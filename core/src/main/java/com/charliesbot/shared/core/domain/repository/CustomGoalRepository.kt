package com.charliesbot.shared.core.domain.repository

import com.charliesbot.shared.core.constants.FastGoal
import kotlinx.coroutines.flow.Flow

interface CustomGoalRepository {
  val customGoals: Flow<List<FastGoal>>

  suspend fun saveCustomGoal(goal: FastGoal, syncToRemote: Boolean = true)

  suspend fun deleteCustomGoal(goalId: String, syncToRemote: Boolean = true)

  suspend fun replaceAllCustomGoals(goals: List<FastGoal>, syncToRemote: Boolean = true)

  suspend fun replaceAllCustomGoalsFromJson(goalsJson: String, syncToRemote: Boolean = true)
}
