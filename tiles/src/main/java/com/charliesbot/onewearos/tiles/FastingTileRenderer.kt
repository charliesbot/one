package com.charliesbot.onewearos.tiles

import android.content.Context
import androidx.compose.ui.graphics.toArgb
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
import androidx.wear.protolayout.material.CircularProgressIndicator
import com.charliesbot.shared.R as SharedR
import com.charliesbot.shared.core.constants.FastGoal
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.utils.FastingProgress
import com.charliesbot.shared.core.utils.formatTimestamp

object FastingTileRenderer {

  private const val MAX_DEGREES = 360f

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

  private fun renderFastingActive(
    context: Context,
    progress: FastingProgress,
    goal: FastGoal,
  ): LayoutElementBuilders.LayoutElement {
    val progressFraction = (progress.progressPercentage / 100f).coerceIn(0f, 1f)
    val arcLength = progressFraction * MAX_DEGREES

    val progressIndicator =
      CircularProgressIndicator.Builder()
        .setStartAngle(-90f)
        .setEndAngle(arcLength - 90f)
        .setCircularProgressIndicatorColors(
          androidx.wear.protolayout.material.ProgressIndicatorColors(
            /* indicatorColor= */ argb(goal.color.toArgb()),
            /* trackColor= */ argb(0xFF303030.toInt()),
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
          FontStyle.Builder().setSize(sp(16f)).setColor(argb(0xFFFFFFFF.toInt())).build()
        )
        .build()

    val goalLabel =
      Text.Builder()
        .setText(goal.durationDisplay)
        .setFontStyle(
          FontStyle.Builder()
            .setSize(sp(24f))
            .setColor(argb(goal.color.toArgb()))
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
          FontStyle.Builder().setSize(sp(14f)).setColor(argb(0xFFAAAAAA.toInt())).build()
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
      .addContent(progressIndicator)
      .addContent(centerContent)
      .build()
  }

  private fun renderNotFasting(context: Context): LayoutElementBuilders.LayoutElement {
    val notFastingText =
      Text.Builder()
        .setText(context.getString(SharedR.string.tile_text_not_fasting))
        .setFontStyle(
          FontStyle.Builder().setSize(sp(18f)).setColor(argb(0xFFFFFFFF.toInt())).build()
        )
        .build()

    return Box.Builder()
      .setWidth(expand())
      .setHeight(expand())
      .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
      .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
      .addContent(notFastingText)
      .build()
  }
}
