package com.charliesbot.shared.core.models

import kotlinx.serialization.Serializable

@Serializable
data class CustomGoalData(
  val id: String,
  val name: String,
  val durationMillis: Long,
  val colorHex: Long,
)
