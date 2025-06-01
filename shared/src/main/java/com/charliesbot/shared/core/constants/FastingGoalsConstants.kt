package com.charliesbot.shared.core.constants

import androidx.compose.ui.graphics.Color


data class FastGoal(
    val id: String,
    val title: String,
    val durationDisplay: String, // e.g., "13", "16" (for display in UI)
    val color: Color,
    val durationMillis: Long, // The actual duration in milliseconds for calculations
)

object PredefinedFastingGoals {

    // Helper to calculate milliseconds from hours
    private fun hoursToMillis(hours: Int): Long = hours * 60L * 60L * 1000L

    val CIRCADIAN = FastGoal(
        id = "circadian",
        title = "Circadian\nRhythm TRF",
        durationDisplay = "13",
        color = Color(0xFF6096BA), // Dusty Blue / Muted Teal
        durationMillis = hoursToMillis(13)
    )

    val SIXTEEN_EIGHT = FastGoal(
        id = "16:8",
        title = "16:8\nTRF",
        durationDisplay = "16",
        color = Color(0xFF82B387), // Muted Sage Green
        durationMillis = hoursToMillis(16),
    )

    val EIGHTEEN_SIX = FastGoal(
        id = "18:6",
        title = "18:6\nTRF",
        durationDisplay = "18",
        color = Color(0xFFE5A98C), // Muted Peach / Terracotta
        durationMillis = hoursToMillis(18)
    )

    val TWENTY_FOUR = FastGoal(
        id = "20:4",
        title = "20:4\nTRF",
        durationDisplay = "20",
        color = Color(0xFFC9AB6A), // Muted Gold / Ochre
        durationMillis = hoursToMillis(20)
    )

    val THIRTY_SIX_HOUR = FastGoal(
        id = "36hour",
        title = "36-Hour\nFast",
        durationDisplay = "36",
        color = Color(0xFF9787BE), // Dusty Lavender / Muted Plum
        durationMillis = hoursToMillis(36)
    )

    val allGoals: List<FastGoal> = listOf(
        CIRCADIAN,
        SIXTEEN_EIGHT,
        EIGHTEEN_SIX,
        TWENTY_FOUR,
        THIRTY_SIX_HOUR
    )

    val goalsById: Map<String, FastGoal> = allGoals.associateBy { it.id }

    val getGoalById = { id: String -> goalsById[id]!! }
}
