package com.charliesbot.one.widget

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
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
import com.charliesbot.one.widget.common.FastingWidgetState
import com.charliesbot.one.widget.common.toFastingWidgetState
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.strings.R as SharedR

@Composable
fun OneWidgetContent(fastingData: FastingDataItem, context: Context) {
  val currentTime = System.currentTimeMillis()
  val selectedGoal = PredefinedFastingGoals.getGoalById(fastingData.fastingGoalId)
  val contentState = fastingData.toFastingWidgetState(currentTime, selectedGoal.durationMillis)

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
        OneWidgetLayoutSize.Minimal -> MinimalWidgetLayout(contentState = contentState, size = size)

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
private fun MinimalWidgetLayout(contentState: FastingWidgetState, size: DpSize) {
  val ringDp = minOf(size.width.value * 0.58f, size.height.value * 0.72f, 72f).dp

  WidgetProgressBar(
    contentState = contentState,
    ringDp = ringDp,
    strokeDp = (ringDp.value * 0.16f).dp,
  )
}

@Composable
private fun CompactWidgetLayout(context: Context, contentState: FastingWidgetState, size: DpSize) {
  val squareSide = minOf(size.width, size.height)
  val showText = squareSide >= OneWidgetSize.TEXT_THRESHOLD
  val ringDp = (squareSide.value * if (showText) 0.35f else 0.65f).dp

  WidgetProgressBar(
    contentState = contentState,
    ringDp = ringDp,
    strokeDp = (squareSide.value * 0.07f).dp,
  )
  if (showText) {
    Spacer(modifier = GlanceModifier.height((squareSide.value * 0.04f).dp))
    WidgetFooter(context = context, contentState = contentState, referenceSize = squareSide)
  }
}

@Composable
private fun WideWidgetLayout(context: Context, contentState: FastingWidgetState, size: DpSize) {
  val ringDp = minOf(size.height.value * 0.62f, 76f).dp

  HorizontalWidgetLayout(
    context = context,
    contentState = contentState,
    ringDp = ringDp,
    strokeDp = (ringDp.value * 0.14f).dp,
    horizontalPadding = 16.dp,
    verticalPadding = 10.dp,
    spacerWidth = 14.dp,
    referenceSize = size.height,
  )
}

@Composable
private fun ExpandedWidgetLayout(context: Context, contentState: FastingWidgetState, size: DpSize) {
  val ringDp = minOf(size.height.value * 0.52f, 104f).dp

  HorizontalWidgetLayout(
    context = context,
    contentState = contentState,
    ringDp = ringDp,
    strokeDp = (ringDp.value * 0.12f).dp,
    horizontalPadding = 22.dp,
    verticalPadding = 18.dp,
    spacerWidth = 18.dp,
    referenceSize = size.height,
  )
}

@Composable
private fun HorizontalWidgetLayout(
  context: Context,
  contentState: FastingWidgetState,
  ringDp: Dp,
  strokeDp: Dp,
  horizontalPadding: Dp,
  verticalPadding: Dp,
  spacerWidth: Dp,
  referenceSize: Dp,
) {
  Row(
    modifier =
      GlanceModifier.fillMaxSize()
        .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    WidgetProgressBar(contentState = contentState, ringDp = ringDp, strokeDp = strokeDp)
    Spacer(modifier = GlanceModifier.width(spacerWidth))
    WidgetFooter(
      context = context,
      contentState = contentState,
      referenceSize = referenceSize,
      alignment = Alignment.Start,
    )
  }
}

@Composable
private fun WidgetProgressBar(contentState: FastingWidgetState, ringDp: Dp, strokeDp: Dp) {
  val context = LocalContext.current
  val progress = contentState.progressFraction

  val trackComposeColor = GlanceTheme.colors.outline.getColor(context).copy(alpha = 0.35f)
  val indicatorComposeColor =
    when {
      contentState.isGoalMet -> GlanceTheme.colors.tertiary.getColor(context)
      contentState.isFasting -> GlanceTheme.colors.primary.getColor(context)
      else -> trackComposeColor
    }

  val density = context.resources.displayMetrics.density

  val ringBitmap =
    remember(progress, indicatorComposeColor, trackComposeColor, density, ringDp, strokeDp) {
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

  val remainingHours = contentState.hoursRemaining.coerceAtLeast(0)
  val contentDescription =
    if (contentState.isFasting) {
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
  contentState: FastingWidgetState,
  referenceSize: Dp,
  alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
) {
  val actualHours = contentState.hoursRemaining.coerceAtLeast(0)

  // Convert dp-based sizes to sp to cancel out font scaling.
  // dp_value / fontScale gives sp that renders at the intended dp size.
  val fontScale = context.resources.configuration.fontScale
  val largeTextSize =
    scaledTextSize(referenceSize, multiplier = 0.22f, min = 20f, max = 60f, fontScale)
  val smallTextSize =
    scaledTextSize(referenceSize, multiplier = 0.09f, min = 8f, max = 24f, fontScale)
  val goalMetTextSize =
    scaledTextSize(referenceSize, multiplier = 0.12f, min = 10f, max = 30f, fontScale)

  when {
    contentState.isFasting && actualHours <= 0 ->
      GoalMetFooter(context = context, textSize = goalMetTextSize, alignment = alignment)

    contentState.isFasting ->
      FastingFooter(
        context = context,
        actualHours = actualHours,
        largeTextSize = largeTextSize,
        smallTextSize = smallTextSize,
        alignment = alignment,
      )

    else -> NotFastingFooter(context = context, textSize = goalMetTextSize)
  }
}

private fun scaledTextSize(
  referenceSize: Dp,
  multiplier: Float,
  min: Float,
  max: Float,
  fontScale: Float,
) = ((referenceSize.value * multiplier).coerceIn(min, max) / fontScale).sp

@Composable
private fun GoalMetFooter(context: Context, textSize: TextUnit, alignment: Alignment.Horizontal) {
  Column(horizontalAlignment = alignment) {
    Text(
      text = context.getString(SharedR.string.widget_goal_met_part_1),
      style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = textSize),
      maxLines = 1,
    )
    Text(
      text = context.getString(SharedR.string.widget_goal_met_part_2),
      style =
        TextStyle(
          color = GlanceTheme.colors.onTertiaryContainer,
          fontSize = textSize,
          fontWeight = FontWeight.Bold,
        ),
      maxLines = 1,
    )
  }
}

@Composable
private fun FastingFooter(
  context: Context,
  actualHours: Long,
  largeTextSize: TextUnit,
  smallTextSize: TextUnit,
  alignment: Alignment.Horizontal,
) {
  Column(horizontalAlignment = alignment) {
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

@Composable
private fun NotFastingFooter(context: Context, textSize: TextUnit) {
  Text(
    text = context.getString(SharedR.string.widget_not_fasting),
    style =
      TextStyle(
        color = GlanceTheme.colors.onSurface,
        fontSize = textSize,
        fontWeight = FontWeight.Bold,
      ),
    maxLines = 1,
  )
}

private val widgetMockFastingData =
  FastingDataItem(
    fastingGoalId = PredefinedFastingGoals.SIXTEEN_EIGHT.id,
    isFasting = true,
    startTimeInMillis = System.currentTimeMillis() - (12 * 60 * 60 * 1000L),
  )

private val widgetMockNotFastingData =
  FastingDataItem(fastingGoalId = PredefinedFastingGoals.SIXTEEN_EIGHT.id, isFasting = false)

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
