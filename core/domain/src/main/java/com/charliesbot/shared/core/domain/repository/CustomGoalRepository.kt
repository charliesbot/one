package com.charliesbot.shared.core.domain.repository

import com.charliesbot.shared.core.models.CustomGoalData
import kotlinx.coroutines.flow.Flow

interface CustomGoalRepository {
  val customGoals: Flow<List<CustomGoalData>>

  suspend fun saveCustomGoal(goal: CustomGoalData, syncToRemote: Boolean = true)

  suspend fun deleteCustomGoal(goalId: String, syncToRemote: Boolean = true)

  suspend fun replaceAllCustomGoals(goals: List<CustomGoalData>, syncToRemote: Boolean = true)

  suspend fun replaceAllCustomGoalsFromJson(goalsJson: String, syncToRemote: Boolean = true)
}
