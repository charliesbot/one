package com.charliesbot.one.widgets

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
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.charliesbot.one.MainActivity
import com.charliesbot.one.R
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.utils.calculateProgressFraction
import com.charliesbot.shared.core.utils.getHours

@Composable
fun OneWidgetContent(
    fastingData: FastingDataItem,
    context: Context
) {
    val currentTime = System.currentTimeMillis()
    val startTimeInMillis = fastingData.startTimeInMillis
    val elapsedMillis = (currentTime - startTimeInMillis).coerceAtLeast(0)
    val selectedGoal = PredefinedFastingGoals.getGoalById(fastingData.fastingGoalId)
    val hours = getHours(selectedGoal.durationMillis) - getHours(elapsedMillis)
    val isGoalMet = fastingData.isFasting && hours <= 0

    val size = LocalSize.current
    val isSmallSquare =
        size.width <= OneWidgetSize.SMALL_SQUARE.width && size.height <= OneWidgetSize.SMALL_SQUARE.height
    val showText = size.width >= OneWidgetSize.HORIZONTAL_RECTANGLE.width
    val useVerticalLayout = size.height >= OneWidgetSize.BIG_SQUARE.height

    // Scale ring size based on available space
    val ringDp = when {
        isSmallSquare -> 36.dp
        size.height <= OneWidgetSize.HORIZONTAL_RECTANGLE.height -> 48.dp
        else -> 60.dp
    }
    val strokeDp = when {
        isSmallSquare -> 5.dp
        size.height <= OneWidgetSize.HORIZONTAL_RECTANGLE.height -> 6.dp
        else -> 10.dp
    }

    // Dynamic padding - minimal for small widgets
    val horizontalPadding = if (isSmallSquare) 4.dp else 16.dp
    val verticalPadding = if (isSmallSquare) 4.dp else if (useVerticalLayout) 24.dp else 8.dp

    GlanceTheme {
        Scaffold(
            modifier = GlanceModifier
                .background(GlanceTheme.colors.widgetBackground)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            if (useVerticalLayout) {
                // Vertical layout for tall widgets (2x2+)
                Column(
                    modifier = GlanceModifier
                        .padding(vertical = verticalPadding, horizontal = horizontalPadding)
                        .fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WidgetProgressBar(
                        isFasting = fastingData.isFasting,
                        elapsedTime = elapsedMillis,
                        fastingGoalMillis = selectedGoal.durationMillis,
                        isGoalMet = isGoalMet,
                        ringDp = ringDp,
                        strokeDp = strokeDp,
                        alignEnd = true
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    WidgetFooter(
                        context,
                        isFasting = fastingData.isFasting,
                        hours = hours,
                        isWide = false
                    )
                }
            } else {
                // Horizontal layout for short widgets
                Row(
                    modifier = GlanceModifier
                        .padding(vertical = verticalPadding, horizontal = horizontalPadding)
                        .fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (showText) {
                        WidgetFooter(
                            context,
                            isFasting = fastingData.isFasting,
                            hours = hours,
                            isWide = true
                        )
                        Spacer(modifier = GlanceModifier.width(16.dp))
                    }
                    WidgetProgressBar(
                        isFasting = fastingData.isFasting,
                        elapsedTime = elapsedMillis,
                        fastingGoalMillis = selectedGoal.durationMillis,
                        isGoalMet = isGoalMet,
                        ringDp = ringDp,
                        strokeDp = strokeDp,
                        alignEnd = false
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
    alignEnd: Boolean
) {
    val context = LocalContext.current
    val progress = calculateProgressFraction(elapsedTime, fastingGoalMillis)

    val trackComposeColor = GlanceTheme.colors.outline.getColor(context).copy(alpha = 0.35f)
    val indicatorComposeColor = if (isGoalMet) {
        GlanceTheme.colors.tertiary.getColor(context)
    } else if (isFasting) {
        GlanceTheme.colors.primary.getColor(context)
    } else {
        trackComposeColor
    }

    // Create bitmap synchronously for preview - this will work in both preview and runtime
    val ringBitmap = remember(progress, indicatorComposeColor, trackComposeColor) {
        val sizePx = (ringDp.value * 3).toInt() // Use a simple multiplier instead of density
        val strokePx = (strokeDp.value * 3)

        ProgressBitmap.draw(
            progress = progress,
            sizePx = sizePx,
            strokePx = strokePx,
            indicator = indicatorComposeColor.toArgb(),
            track = trackComposeColor.toArgb()
        )
    }

    Row(
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        modifier = if (alignEnd) GlanceModifier.fillMaxWidth() else GlanceModifier
    ) {
        Image(
            provider = ImageProvider(ringBitmap),
            contentDescription = context.getString(R.string.progress_ring_desc),
            modifier = GlanceModifier.size(ringDp)
        )
    }
}

@Composable
private fun WidgetFooter(
    context: Context,
    isFasting: Boolean,
    hours: Long,
    isWide: Boolean
) {
    val actualHours = hours.coerceAtLeast(0)

    val largeTextSize = if (isWide) 32.sp else 60.sp
    val smallTextSize = if (isWide) 12.sp else 20.sp
    val goalMetTextSize = if (isWide) 14.sp else 30.sp

    if (isFasting && actualHours <= 0) {
        return Row {
            Text(
                text = context.getString(R.string.widget_goal_met_part_1),
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontSize = goalMetTextSize,
                )
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = context.getString(R.string.widget_goal_met_part_2),
                style = TextStyle(
                    color = GlanceTheme.colors.onTertiaryContainer,
                    fontSize = goalMetTextSize,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    if (isFasting) {
        return Column() {
            Text(
                text = actualHours.toString(),
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = largeTextSize,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = context.resources.getQuantityString(
                    R.plurals.widget_hours_left_plural,
                    actualHours.toInt()
                ),
                style = TextStyle(
                    color = GlanceTheme.colors.secondary,
                    fontSize = smallTextSize,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    return Text(
        text = context.getString(R.string.widget_not_fasting),
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = if (isWide) 18.sp else 24.sp,
            fontWeight = FontWeight.Bold
        )
    )
}

val widgetMockFastingData = FastingDataItem(
    fastingGoalId = PredefinedFastingGoals.SIXTEEN_EIGHT.id,
    isFasting = true,
    startTimeInMillis = System.currentTimeMillis() - (12 * 60 * 60 * 1000L) // 12 hours ago
)

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 57, heightDp = 57)   // Small square (1x1)
@Preview(widthDp = 130, heightDp = 57)  // Horizontal rectangle (Nx1)
@Preview(widthDp = 130, heightDp = 130) // Big square (2x2+)
@Composable
private fun OneWidgetContentPreview() {
    OneWidgetContent(
        fastingData = widgetMockFastingData,
        context = LocalContext.current
    )
}
