package com.charliesbot.shared.core.designsystem.common.goals

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.charliesbot.shared.core.models.CUSTOM_GOAL_ID_PREFIX
import com.charliesbot.shared.core.models.FastingGoal
import com.charliesbot.shared.core.models.FastingGoalCatalog
import com.charliesbot.shared.core.strings.R

data class FastGoal(
  val id: String,
  val titleResId: Int = 0,
  val titleText: String? = null,
  val durationDisplay: String,
  val color: Color,
  val durationMillis: Long,
) {
  val isCustom: Boolean
    get() = id.startsWith(CUSTOM_GOAL_ID_PREFIX)

  fun getTitle(context: Context): String = titleText ?: context.getString(titleResId)
}

object PredefinedFastingGoals {

  private const val LOG_TAG = "ONE_FASTING_APP"

  const val MIN_FASTING_HOURS = FastingGoalCatalog.MIN_FASTING_HOURS

  val CIRCADIAN =
    FastingGoalCatalog.CIRCADIAN.toFastGoal(
      titleResId = R.string.fasting_goal_circadian,
      color = Color(0xFF6096BA),
    )

  val SIXTEEN_EIGHT =
    FastingGoalCatalog.SIXTEEN_EIGHT.toFastGoal(
      titleResId = R.string.fasting_goal_16_8,
      color = Color(0xFF82B387),
    )

  val EIGHTEEN_SIX =
    FastingGoalCatalog.EIGHTEEN_SIX.toFastGoal(
      titleResId = R.string.fasting_goal_18_6,
      color = Color(0xFFE5A98C),
    )

  val TWENTY_FOUR =
    FastingGoalCatalog.TWENTY_FOUR.toFastGoal(
      titleResId = R.string.fasting_goal_20_4,
      color = Color(0xFFC9AB6A),
    )

  val THIRTY_SIX_HOUR =
    FastingGoalCatalog.THIRTY_SIX_HOUR.toFastGoal(
      titleResId = R.string.fasting_goal_36_hour,
      color = Color(0xFF9787BE),
    )

  val allGoals: List<FastGoal> =
    listOf(CIRCADIAN, SIXTEEN_EIGHT, EIGHTEEN_SIX, TWENTY_FOUR, THIRTY_SIX_HOUR)

  val goalsById: Map<String, FastGoal> = allGoals.associateBy { it.id }

  val customGoalColors: List<Color> =
    listOf(
      Color(0xFFB07AA1),
      Color(0xFF7BA3A8),
      Color(0xFFD4956B),
      Color(0xFF8A9A5B),
      Color(0xFF9E8B70),
      Color(0xFF6C8EBF),
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
        Log.e(LOG_TAG, "Invalid goal id: $id")
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
