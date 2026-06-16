package com.charliesbot.one.widget.wear

import android.content.Context
import com.charliesbot.one.widget.common.FastingWidgetState
import com.charliesbot.shared.core.strings.R as SharedR

internal fun FastingWidgetState.toWearWidgetContent(context: Context): WearWidgetContent =
  toWearWidgetContent(
    notFastingText = context.getString(SharedR.string.widget_not_fasting),
    goalMetPartOne = context.getString(SharedR.string.widget_goal_met_part_1),
    goalMetPartTwo = context.getString(SharedR.string.widget_goal_met_part_2),
    hoursLeftText = { hours ->
      context.resources.getQuantityString(SharedR.plurals.widget_hours_left_plural, hours)
    },
  )

internal fun FastingWidgetState.toWearWidgetContent(
  notFastingText: String,
  goalMetPartOne: String,
  goalMetPartTwo: String,
  hoursLeftText: (Int) -> String,
): WearWidgetContent {
  val hoursRemaining = hoursRemaining.coerceAtLeast(0).toInt()

  return when {
    isFasting && isGoalMet ->
      WearWidgetContent.Fasting(primaryText = goalMetPartOne, secondaryText = goalMetPartTwo)

    isFasting ->
      WearWidgetContent.Fasting(
        primaryText = hoursRemaining.toString(),
        secondaryText = hoursLeftText(hoursRemaining),
      )

    else -> WearWidgetContent.NotFasting(text = notFastingText)
  }
}

internal sealed interface WearWidgetContent {
  data class NotFasting(val text: String) : WearWidgetContent

  data class Fasting(val primaryText: String, val secondaryText: String) : WearWidgetContent
}
