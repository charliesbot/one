package com.charliesbot.shared.core.constants

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.charliesbot.shared.R

data class FastGoal(
    val id: String,
    val titleResId: Int,
    val durationDisplay: String, // e.g., "13", "16" (for display in UI)
    val color: Color,
    val durationMillis: Long, // The actual duration in milliseconds for calculations
) {
    fun getTitle(context: android.content.Context): String {
        return context.getString(titleResId)
    }
}

object PredefinedFastingGoals {

    // Helper to calculate milliseconds from hours
    private fun hoursToMillis(hours: Int): Long = hours * 60L * 60L * 1000L

    val CIRCADIAN = FastGoal(
        id = "circadian",
        titleResId = R.string.fasting_goal_circadian,
        durationDisplay = "13",
        color = Color(0xFF6096BA), // Dusty Blue / Muted Teal
        durationMillis = hoursToMillis(13)
    )

    val SIXTEEN_EIGHT = FastGoal(
        id = "16:8",
        titleResId = R.string.fasting_goal_16_8,
        durationDisplay = "16",
        color = Color(0xFF82B387), // Muted Sage Green
        durationMillis = hoursToMillis(16),
    )

    val EIGHTEEN_SIX = FastGoal(
        id = "18:6",
        titleResId = R.string.fasting_goal_18_6,
        durationDisplay = "18",
        color = Color(0xFFE5A98C), // Muted Peach / Terracotta
        durationMillis = hoursToMillis(18)
    )

    val TWENTY_FOUR = FastGoal(
        id = "20:4",
        titleResId = R.string.fasting_goal_20_4,
        durationDisplay = "20",
        color = Color(0xFFC9AB6A), // Muted Gold / Ochre
        durationMillis = hoursToMillis(20)
    )

    val THIRTY_SIX_HOUR = FastGoal(
        id = "36hour",
        titleResId = R.string.fasting_goal_36_hour,
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

    val getGoalById: (String) -> FastGoal = { id: String ->
        if (!goalsById.containsKey(id)) {
            Log.e(AppConstants.LOG_TAG, "Invalid goal id: $id")
            goalsById["16:8"]!!
        } else {
            goalsById[id]!!
        }
    }
}
