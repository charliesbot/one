package com.charliesbot.shared.core.utils

import com.charliesbot.shared.core.constants.FastGoal
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.domain.repository.CustomGoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GoalResolver(customGoalRepository: CustomGoalRepository) {
  val allGoals: Flow<List<FastGoal>> =
    customGoalRepository.customGoals.map { customGoals ->
      PredefinedFastingGoals.registerCustomGoals(customGoals)
      PredefinedFastingGoals.allGoals + customGoals
    }
}
