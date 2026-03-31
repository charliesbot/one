package com.charliesbot.one.widget

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
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
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.charliesbot.shared.R as SharedR
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.utils.calculateProgressFraction
import com.charliesbot.shared.core.utils.getHours

@Composable
fun OneWidgetContent(fastingData: FastingDataItem, context: Context) {
  val currentTime = System.currentTimeMillis()
  val startTimeInMillis = fastingData.startTimeInMillis
  val elapsedMillis = (currentTime - startTimeInMillis).coerceAtLeast(0)
  val selectedGoal = PredefinedFastingGoals.getGoalById(fastingData.fastingGoalId)
  val hours = getHours(selectedGoal.durationMillis) - getHours(elapsedMillis)
  val isGoalMet = fastingData.isFasting && hours <= 0

  val size = LocalSize.current
  val squareSide = minOf(size.width, size.height)
  val showText = squareSide >= OneWidgetSize.TEXT_THRESHOLD

  val ringDp = (squareSide.value * if (showText) 0.35f else 0.65f).dp
  val strokeDp = (squareSide.value * 0.07f).dp

  GlanceTheme {
    Row(
      modifier = GlanceModifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier =
          GlanceModifier.size(squareSide)
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
        WidgetProgressBar(
          isFasting = fastingData.isFasting,
          elapsedTime = elapsedMillis,
          fastingGoalMillis = selectedGoal.durationMillis,
          isGoalMet = isGoalMet,
          ringDp = ringDp,
          strokeDp = strokeDp,
        )
        if (showText) {
          Spacer(modifier = GlanceModifier.height((squareSide.value * 0.04f).dp))
          WidgetFooter(
            context = context,
            isFasting = fastingData.isFasting,
            hours = hours,
            squareSide = squareSide,
          )
        }
      }
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
private fun WidgetFooter(context: Context, isFasting: Boolean, hours: Long, squareSide: Dp) {
  val actualHours = hours.coerceAtLeast(0)

  // Convert dp-based sizes to sp to cancel out font scaling.
  // dp_value / fontScale gives sp that renders at the intended dp size.
  val fontScale = context.resources.configuration.fontScale
  val largeTextSize = ((squareSide.value * 0.22f).coerceIn(20f, 60f) / fontScale).sp
  val smallTextSize = ((squareSide.value * 0.09f).coerceIn(8f, 24f) / fontScale).sp
  val goalMetTextSize = ((squareSide.value * 0.12f).coerceIn(10f, 30f) / fontScale).sp

  if (isFasting && actualHours <= 0) {
    return Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = context.getString(SharedR.string.widget_goal_met_part_1),
        style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = goalMetTextSize),
      )
      Text(
        text = context.getString(SharedR.string.widget_goal_met_part_2),
        style =
          TextStyle(
            color = GlanceTheme.colors.onTertiaryContainer,
            fontSize = goalMetTextSize,
            fontWeight = FontWeight.Bold,
          ),
      )
    }
  }

  if (isFasting) {
    return Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = actualHours.toString(),
        style =
          TextStyle(
            color = GlanceTheme.colors.primary,
            fontSize = largeTextSize,
            fontWeight = FontWeight.Bold,
          ),
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
  )
}

val widgetMockFastingData =
  FastingDataItem(
    fastingGoalId = PredefinedFastingGoals.SIXTEEN_EIGHT.id,
    isFasting = true,
    startTimeInMillis = System.currentTimeMillis() - (12 * 60 * 60 * 1000L),
  )

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 57, heightDp = 57)
@Preview(widthDp = 200, heightDp = 57)
@Preview(widthDp = 130, heightDp = 130)
@Preview(widthDp = 300, heightDp = 130)
@Composable
private fun OneWidgetContentPreview() {
  OneWidgetContent(fastingData = widgetMockFastingData, context = LocalContext.current)
}
