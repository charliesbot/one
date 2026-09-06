package com.charliesbot.shared.core.utils

import com.charliesbot.shared.core.domain.repository.CustomGoalRepository
import com.charliesbot.shared.core.models.FastingGoal
import com.charliesbot.shared.core.models.FastingGoalCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class GoalResolver(customGoalRepository: CustomGoalRepository) {
  val allGoals: Flow<List<FastingGoal>> =
    customGoalRepository.customGoals.map { customGoalData ->
      FastingGoalCatalog.allGoals + customGoalData.map { it.toFastingGoal() }
    }

  suspend fun resolveGoalDurationMillis(goalId: String): Long {
    val customMatch = allGoals.first().find { it.id == goalId }
    return customMatch?.durationMillis ?: FastingGoalCatalog.getGoalById(goalId).durationMillis
  }
}
