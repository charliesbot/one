package com.charliesbot.shared.core.models

object FastingGoalCatalog {
  const val MIN_FASTING_HOURS = FastingRules.MINIMUM_COMPLETED_FAST_HOURS

  const val CIRCADIAN_ID = "circadian"
  const val SIXTEEN_EIGHT_ID = "16:8"
  const val EIGHTEEN_SIX_ID = "18:6"
  const val TWENTY_FOUR_ID = "20:4"
  const val THIRTY_SIX_HOUR_ID = "36hour"
  const val DEFAULT_GOAL_ID = SIXTEEN_EIGHT_ID

  val CIRCADIAN = predefinedGoal(CIRCADIAN_ID, hours = 13)
  val SIXTEEN_EIGHT = predefinedGoal(SIXTEEN_EIGHT_ID, hours = 16)
  val EIGHTEEN_SIX = predefinedGoal(EIGHTEEN_SIX_ID, hours = 18)
  val TWENTY_FOUR = predefinedGoal(TWENTY_FOUR_ID, hours = 20)
  val THIRTY_SIX_HOUR = predefinedGoal(THIRTY_SIX_HOUR_ID, hours = 36)

  val allGoals: List<FastingGoal> =
    listOf(CIRCADIAN, SIXTEEN_EIGHT, EIGHTEEN_SIX, TWENTY_FOUR, THIRTY_SIX_HOUR)

  val goalsById: Map<String, FastingGoal> = allGoals.associateBy { it.id }

  fun getGoalById(id: String): FastingGoal = goalsById[id] ?: SIXTEEN_EIGHT

  private fun predefinedGoal(id: String, hours: Int): FastingGoal =
    FastingGoal(id = id, durationMillis = hours * MILLIS_PER_HOUR)
}

private const val MILLIS_PER_HOUR = 60L * 60L * 1000L
