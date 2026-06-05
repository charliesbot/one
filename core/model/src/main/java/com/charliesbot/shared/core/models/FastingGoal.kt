package com.charliesbot.shared.core.models

data class FastingGoal(val id: String, val name: String? = null, val durationMillis: Long) {
  val durationDisplay: String
    get() = (durationMillis / MILLIS_PER_HOUR).toString()

  val isCustom: Boolean
    get() = id.startsWith(CUSTOM_GOAL_ID_PREFIX)
}

const val CUSTOM_GOAL_ID_PREFIX = "custom_"

private const val MILLIS_PER_HOUR = 60L * 60L * 1000L
