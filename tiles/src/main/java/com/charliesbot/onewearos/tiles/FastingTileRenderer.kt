package com.charliesbot.onewearos.tiles

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.material.CircularProgressIndicator
import com.charliesbot.shared.R as SharedR
import com.charliesbot.shared.core.constants.FastGoal
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.utils.FastingProgress
import com.charliesbot.shared.core.utils.formatTimestamp

object FastingTileRenderer {

  private const val MAX_DEGREES = 360f

  private const val COLOR_TRACK = 0xFF303030.toInt()
  private const val COLOR_TEXT_PRIMARY = 0xFFFFFFFF.toInt()
  private const val COLOR_TEXT_SECONDARY = 0xFFAAAAAA.toInt()

  private const val LAUNCH_PACKAGE = "com.charliesbot.one"
  private const val LAUNCH_ACTIVITY = "com.charliesbot.onewearos.presentation.MainActivity"

  private fun createTapClickable(): ModifiersBuilders.Clickable {
    return ModifiersBuilders.Clickable.Builder()
      .setId("open_app")
      .setOnClick(
        ActionBuilders.LaunchAction.Builder()
          .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder()
              .setPackageName(LAUNCH_PACKAGE)
              .setClassName(LAUNCH_ACTIVITY)
              .build()
          )
          .build()
      )
      .build()
  }

  fun renderTile(
    context: Context,
    fastingDataItem: FastingDataItem,
    fastingProgress: FastingProgress?,
    fastingGoal: FastGoal?,
  ): LayoutElementBuilders.LayoutElement {
    return if (fastingDataItem.isFasting && fastingProgress != null && fastingGoal != null) {
      renderFastingActive(context, fastingProgress, fastingGoal)
    } else {
      renderNotFasting(context)
    }
  }

  private fun goalColorToArgb(goal: FastGoal): Int {
    return android.graphics.Color.argb(
      goal.color.alpha,
      goal.color.red,
      goal.color.green,
      goal.color.blue,
    )
  }

  private fun renderFastingActive(
    context: Context,
    progress: FastingProgress,
    goal: FastGoal,
  ): LayoutElementBuilders.LayoutElement {
    val progressFraction = (progress.progressPercentage / 100f).coerceIn(0f, 1f)
    val arcLength = progressFraction * MAX_DEGREES
    val goalArgb = goalColorToArgb(goal)

    val progressIndicator =
      CircularProgressIndicator.Builder()
        .setStartAngle(-90f)
        .setEndAngle(arcLength - 90f)
        .setCircularProgressIndicatorColors(
          androidx.wear.protolayout.material.ProgressIndicatorColors(
            /* indicatorColor= */ argb(goalArgb),
            /* trackColor= */ argb(COLOR_TRACK),
          )
        )
        .build()

    val elapsedText =
      Text.Builder()
        .setText(
          context.getString(
            SharedR.string.tile_elapsed_format,
            formatTimestamp(progress.elapsedTimeMillis),
          )
        )
        .setFontStyle(
          FontStyle.Builder().setSize(sp(16f)).setColor(argb(COLOR_TEXT_PRIMARY)).build()
        )
        .build()

    val goalLabel =
      Text.Builder()
        .setText(goal.durationDisplay)
        .setFontStyle(
          FontStyle.Builder()
            .setSize(sp(24f))
            .setColor(argb(goalArgb))
            .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
            .build()
        )
        .build()

    val remainingText =
      Text.Builder()
        .setText(
          context.getString(
            SharedR.string.tile_remaining_format,
            formatTimestamp(progress.remainingTimeMillis),
          )
        )
        .setFontStyle(
          FontStyle.Builder().setSize(sp(14f)).setColor(argb(COLOR_TEXT_SECONDARY)).build()
        )
        .build()

    val centerContent =
      Column.Builder()
        .setWidth(expand())
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        .addContent(elapsedText)
        .addContent(Spacer.Builder().setHeight(dp(4f)).build())
        .addContent(goalLabel)
        .addContent(Spacer.Builder().setHeight(dp(4f)).build())
        .addContent(remainingText)
        .build()

    return Box.Builder()
      .setWidth(expand())
      .setHeight(expand())
      .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
      .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
      .setModifiers(
        ModifiersBuilders.Modifiers.Builder().setClickable(createTapClickable()).build()
      )
      .addContent(progressIndicator)
      .addContent(centerContent)
      .build()
  }

  private fun renderNotFasting(context: Context): LayoutElementBuilders.LayoutElement {
    val notFastingText =
      Text.Builder()
        .setText(context.getString(SharedR.string.tile_text_not_fasting))
        .setFontStyle(
          FontStyle.Builder().setSize(sp(18f)).setColor(argb(COLOR_TEXT_PRIMARY)).build()
        )
        .build()

    return Box.Builder()
      .setWidth(expand())
      .setHeight(expand())
      .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
      .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
      .setModifiers(
        ModifiersBuilders.Modifiers.Builder().setClickable(createTapClickable()).build()
      )
      .addContent(notFastingText)
      .build()
  }
}
