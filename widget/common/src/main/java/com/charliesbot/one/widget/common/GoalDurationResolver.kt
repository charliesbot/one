package com.charliesbot.one.widget.common

fun interface GoalDurationResolver {
  suspend fun durationMillis(goalId: String): Long
}
