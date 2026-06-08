package com.charliesbot.one.widget

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.domain.progress.calculateProgressFraction
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.strings.R as SharedR
import com.charliesbot.shared.core.utils.getHours

private data class WidgetContentState(
  val fastingData: FastingDataItem,
  val elapsedMillis: Long,
  val fastingGoalMillis: Long,
  val hours: Long,
  val isGoalMet: Boolean,
)

@Composable
fun OneWidgetContent(fastingData: FastingDataItem, context: Context) {
  val currentTime = System.currentTimeMillis()
  val startTimeInMillis = fastingData.startTimeInMillis
  val elapsedMillis = (currentTime - startTimeInMillis).coerceAtLeast(0)
  val selectedGoal = PredefinedFastingGoals.getGoalById(fastingData.fastingGoalId)
  val hours = getHours(selectedGoal.durationMillis) - getHours(elapsedMillis)
  val contentState =
    WidgetContentState(
      fastingData = fastingData,
      elapsedMillis = elapsedMillis,
      fastingGoalMillis = selectedGoal.durationMillis,
      hours = hours,
      isGoalMet = fastingData.isFasting && hours <= 0,
    )

  val size = LocalSize.current
  val layoutSize = OneWidgetSize.layoutFor(size)

  GlanceTheme {
    Column(
      modifier =
        GlanceModifier.fillMaxSize()
          .cornerRadius(16.dp)
          .background(GlanceTheme.colors.widgetBackground)
          .clickable(
            actionStartActivity(
              ComponentName(context.packageName, "com.charliesbot.one.MainActivity")
            )
          ),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      when (layoutSize) {
        OneWidgetLayoutSize.Minimal ->
          MinimalWidgetLayout(contentState = contentState, size = size)

        OneWidgetLayoutSize.Compact ->
          CompactWidgetLayout(context = context, contentState = contentState, size = size)

        OneWidgetLayoutSize.Wide ->
          WideWidgetLayout(context = context, contentState = contentState, size = size)

        OneWidgetLayoutSize.Expanded ->
          ExpandedWidgetLayout(context = context, contentState = contentState, size = size)
      }
    }
  }
}

@Composable
private fun MinimalWidgetLayout(contentState: WidgetContentState, size: DpSize) {
  val ringDp = minOf(size.width.value * 0.58f, size.height.value * 0.72f, 72f).dp

  WidgetProgressBar(
    isFasting = contentState.fastingData.isFasting,
    elapsedTime = contentState.elapsedMillis,
    fastingGoalMillis = contentState.fastingGoalMillis,
    isGoalMet = contentState.isGoalMet,
    ringDp = ringDp,
    strokeDp = (ringDp.value * 0.16f).dp,
  )
}

@Composable
private fun CompactWidgetLayout(
  context: Context,
  contentState: WidgetContentState,
  size: DpSize,
) {
  val squareSide = minOf(size.width, size.height)
  val showText = squareSide >= OneWidgetSize.TEXT_THRESHOLD
  val ringDp = (squareSide.value * if (showText) 0.35f else 0.65f).dp

  WidgetProgressBar(
    isFasting = contentState.fastingData.isFasting,
    elapsedTime = contentState.elapsedMillis,
    fastingGoalMillis = contentState.fastingGoalMillis,
    isGoalMet = contentState.isGoalMet,
    ringDp = ringDp,
    strokeDp = (squareSide.value * 0.07f).dp,
  )
  if (showText) {
    Spacer(modifier = GlanceModifier.height((squareSide.value * 0.04f).dp))
    WidgetFooter(
      context = context,
      isFasting = contentState.fastingData.isFasting,
      hours = contentState.hours,
      referenceSize = squareSide,
    )
  }
}

@Composable
private fun WideWidgetLayout(
  context: Context,
  contentState: WidgetContentState,
  size: DpSize,
) {
  val ringDp = minOf(size.height.value * 0.62f, 76f).dp

  Row(
    modifier = GlanceModifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    WidgetProgressBar(
      isFasting = contentState.fastingData.isFasting,
      elapsedTime = contentState.elapsedMillis,
      fastingGoalMillis = contentState.fastingGoalMillis,
      isGoalMet = contentState.isGoalMet,
      ringDp = ringDp,
      strokeDp = (ringDp.value * 0.14f).dp,
    )
    Spacer(modifier = GlanceModifier.width(14.dp))
    WidgetFooter(
      context = context,
      isFasting = contentState.fastingData.isFasting,
      hours = contentState.hours,
      referenceSize = size.height,
      alignment = Alignment.Start,
    )
  }
}

@Composable
private fun ExpandedWidgetLayout(
  context: Context,
  contentState: WidgetContentState,
  size: DpSize,
) {
  val ringDp = minOf(size.height.value * 0.52f, 104f).dp

  Row(
    modifier = GlanceModifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    WidgetProgressBar(
      isFasting = contentState.fastingData.isFasting,
      elapsedTime = contentState.elapsedMillis,
      fastingGoalMillis = contentState.fastingGoalMillis,
      isGoalMet = contentState.isGoalMet,
      ringDp = ringDp,
      strokeDp = (ringDp.value * 0.12f).dp,
    )
    Spacer(modifier = GlanceModifier.width(18.dp))
    Column(horizontalAlignment = Alignment.Start, verticalAlignment = Alignment.CenterVertically) {
      WidgetFooter(
        context = context,
        isFasting = contentState.fastingData.isFasting,
        hours = contentState.hours,
        referenceSize = size.height,
        alignment = Alignment.Start,
      )
    }
  }
}

@Composable
private fun WidgetProgressBar(
  isFasting: Boolean,
  elapsedTime: Long,
  fastingGoalMillis: Long,
  isGoalMet: Boolean,
  ringDp: Dp,
  strokeDp: Dp,
) {
  val context = LocalContext.current
  val progress = calculateProgressFraction(elapsedTime, fastingGoalMillis)

  val trackComposeColor = GlanceTheme.colors.outline.getColor(context).copy(alpha = 0.35f)
  val indicatorComposeColor =
    if (isGoalMet) {
      GlanceTheme.colors.tertiary.getColor(context)
    } else if (isFasting) {
      GlanceTheme.colors.primary.getColor(context)
    } else {
      trackComposeColor
    }

  val density = context.resources.displayMetrics.density

  val ringBitmap =
    remember(progress, indicatorComposeColor, trackComposeColor, density) {
      val sizePx = (ringDp.value * density).toInt()
      val strokePx = (strokeDp.value * density)

      ProgressBitmap.draw(
        progress = progress,
        sizePx = sizePx,
        strokePx = strokePx,
        indicator = indicatorComposeColor.toArgb(),
        track = trackComposeColor.toArgb(),
      )
    }

  val remainingHours = (getHours(fastingGoalMillis) - getHours(elapsedTime)).coerceAtLeast(0)
  val contentDescription =
    if (isFasting) {
      context.getString(
        SharedR.string.accessibility_fasting_status,
        (progress * 100).toInt(),
        remainingHours,
      )
    } else {
      context.getString(SharedR.string.widget_not_fasting)
    }

  Image(
    provider = ImageProvider(ringBitmap),
    contentDescription = contentDescription,
    modifier = GlanceModifier.size(ringDp),
  )
}

@Composable
private fun WidgetFooter(
  context: Context,
  isFasting: Boolean,
  hours: Long,
  referenceSize: Dp,
  alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
) {
  val actualHours = hours.coerceAtLeast(0)

  // Convert dp-based sizes to sp to cancel out font scaling.
  // dp_value / fontScale gives sp that renders at the intended dp size.
  val fontScale = context.resources.configuration.fontScale
  val largeTextSize = ((referenceSize.value * 0.22f).coerceIn(20f, 60f) / fontScale).sp
  val smallTextSize = ((referenceSize.value * 0.09f).coerceIn(8f, 24f) / fontScale).sp
  val goalMetTextSize = ((referenceSize.value * 0.12f).coerceIn(10f, 30f) / fontScale).sp

  if (isFasting && actualHours <= 0) {
    return Column(horizontalAlignment = alignment) {
      Text(
        text = context.getString(SharedR.string.widget_goal_met_part_1),
        style =
          TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = goalMetTextSize),
        maxLines = 1,
      )
      Text(
        text = context.getString(SharedR.string.widget_goal_met_part_2),
        style =
          TextStyle(
            color = GlanceTheme.colors.onTertiaryContainer,
            fontSize = goalMetTextSize,
            fontWeight = FontWeight.Bold,
          ),
        maxLines = 1,
      )
    }
  }

  if (isFasting) {
    return Column(horizontalAlignment = alignment) {
      Text(
        text = actualHours.toString(),
        style =
          TextStyle(
            color = GlanceTheme.colors.primary,
            fontSize = largeTextSize,
            fontWeight = FontWeight.Bold,
          ),
        maxLines = 1,
      )
      Text(
        text =
          context.resources.getQuantityString(
            SharedR.plurals.widget_hours_left_plural,
            actualHours.toInt(),
          ),
        style =
          TextStyle(
            color = GlanceTheme.colors.secondary,
            fontSize = smallTextSize,
            fontWeight = FontWeight.Bold,
          ),
        maxLines = 1,
      )
    }
  }

  return Text(
    text = context.getString(SharedR.string.widget_not_fasting),
    style =
      TextStyle(
        color = GlanceTheme.colors.onSurface,
        fontSize = goalMetTextSize,
        fontWeight = FontWeight.Bold,
      ),
    maxLines = 1,
  )
}

val widgetMockFastingData =
  FastingDataItem(
    fastingGoalId = PredefinedFastingGoals.SIXTEEN_EIGHT.id,
    isFasting = true,
    startTimeInMillis = System.currentTimeMillis() - (12 * 60 * 60 * 1000L),
  )

val widgetMockNotFastingData =
  FastingDataItem(
    fastingGoalId = PredefinedFastingGoals.SIXTEEN_EIGHT.id,
    isFasting = false,
  )

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 57, heightDp = 57)
@Preview(widthDp = 109, heightDp = 56)
@Preview(widthDp = 130, heightDp = 130)
@Preview(widthDp = 245, heightDp = 56)
@Preview(widthDp = 245, heightDp = 115)
@Preview(widthDp = 300, heightDp = 180)
private annotation class OneWidgetSizePreviews

@OneWidgetSizePreviews
@Composable
private fun OneWidgetContentPreview() {
  OneWidgetContent(fastingData = widgetMockFastingData, context = LocalContext.current)
}

@OneWidgetSizePreviews
@Composable
private fun OneWidgetContentNotFastingPreview() {
  OneWidgetContent(fastingData = widgetMockNotFastingData, context = LocalContext.current)
}
