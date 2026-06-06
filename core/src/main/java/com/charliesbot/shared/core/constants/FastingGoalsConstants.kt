package com.charliesbot.shared.core.constants

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.charliesbot.shared.core.strings.R
import com.charliesbot.shared.core.models.CUSTOM_GOAL_ID_PREFIX
import com.charliesbot.shared.core.models.FastingGoal
import com.charliesbot.shared.core.models.FastingGoalCatalog

data class FastGoal(
  val id: String,
  val titleResId: Int = 0,
  val titleText: String? = null,
  val durationDisplay: String, // e.g., "13", "16" (for display in UI)
  val color: Color,
  val durationMillis: Long, // The actual duration in milliseconds for calculations
) {
  val isCustom: Boolean
    get() = id.startsWith(CUSTOM_GOAL_ID_PREFIX)

  fun getTitle(context: android.content.Context): String =
    titleText ?: context.getString(titleResId)
}

object PredefinedFastingGoals {

  const val MIN_FASTING_HOURS = FastingGoalCatalog.MIN_FASTING_HOURS

  val CIRCADIAN =
    FastingGoalCatalog.CIRCADIAN.toFastGoal(
      titleResId = R.string.fasting_goal_circadian,
      color = Color(0xFF6096BA), // Dusty Blue / Muted Teal
    )

  val SIXTEEN_EIGHT =
    FastingGoalCatalog.SIXTEEN_EIGHT.toFastGoal(
      titleResId = R.string.fasting_goal_16_8,
      color = Color(0xFF82B387), // Muted Sage Green
    )

  val EIGHTEEN_SIX =
    FastingGoalCatalog.EIGHTEEN_SIX.toFastGoal(
      titleResId = R.string.fasting_goal_18_6,
      color = Color(0xFFE5A98C), // Muted Peach / Terracotta
    )

  val TWENTY_FOUR =
    FastingGoalCatalog.TWENTY_FOUR.toFastGoal(
      titleResId = R.string.fasting_goal_20_4,
      color = Color(0xFFC9AB6A), // Muted Gold / Ochre
    )

  val THIRTY_SIX_HOUR =
    FastingGoalCatalog.THIRTY_SIX_HOUR.toFastGoal(
      titleResId = R.string.fasting_goal_36_hour,
      color = Color(0xFF9787BE), // Dusty Lavender / Muted Plum
    )

  val allGoals: List<FastGoal> =
    listOf(CIRCADIAN, SIXTEEN_EIGHT, EIGHTEEN_SIX, TWENTY_FOUR, THIRTY_SIX_HOUR)

  val goalsById: Map<String, FastGoal> = allGoals.associateBy { it.id }

  val customGoalColors: List<Color> =
    listOf(
      Color(0xFFB07AA1), // Muted Mauve
      Color(0xFF7BA3A8), // Teal Slate
      Color(0xFFD4956B), // Warm Amber
      Color(0xFF8A9A5B), // Olive Green
      Color(0xFF9E8B70), // Taupe
      Color(0xFF6C8EBF), // Steel Blue
    )

  @Volatile private var customGoalsMap: Map<String, FastGoal> = emptyMap()

  fun registerCustomGoals(goals: List<FastGoal>) {
    customGoalsMap = goals.associateBy { it.id }
  }

  val getGoalById: (String) -> FastGoal = { id: String ->
    val predefined = goalsById[id]
    val custom = customGoalsMap[id]
    when {
      predefined != null -> predefined

      custom != null -> custom

      else -> {
        Log.e(AppConstants.LOG_TAG, "Invalid goal id: $id")
        goalsById[FastingGoalCatalog.DEFAULT_GOAL_ID]!!
      }
    }
  }

  private fun FastingGoal.toFastGoal(titleResId: Int, color: Color): FastGoal =
    FastGoal(
      id = id,
      titleResId = titleResId,
      durationDisplay = durationDisplay,
      color = color,
      durationMillis = durationMillis,
    )
}
