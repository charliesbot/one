package com.charliesbot.shared.core.models


data class FastingDataItem(
    val isFasting: Boolean = false,
    val startTimeInMillis: Long = 0L,
    val updateTimestamp: Long = 0L,
    val fastingGoalId: String = "",
)