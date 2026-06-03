package com.charliesbot.shared.core.models

import java.time.LocalDate

data class FastingDayData(
  val date: LocalDate,
  val durationHours: Int? = null,
  val isGoalMet: Boolean = false,
  val startTimeEpochMillis: Long? = null,
  val endTimeEpochMillis: Long? = null,
  val goalId: String? = null,
)
