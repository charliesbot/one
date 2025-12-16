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
    // Wide: Width is generous, Height is constrained (e.g. 300x120)
    val isWide = size.width >= 250.dp && size.height < 160.dp
    // Compact: Small dimensions (e.g. 120x120)
    val isCompact = !isWide && (size.width < 160.dp || size.height < 160.dp)

    val ringDp = if (isCompact) 48.dp else if (isWide) 56.dp else 60.dp
    val strokeDp = if (isCompact) 6.dp else if (isWide) 8.dp else 10.dp
    val verticalPadding = if (isCompact || isWide) 8.dp else 24.dp

    GlanceTheme {
        Scaffold(
            modifier = GlanceModifier
                .background(GlanceTheme.colors.widgetBackground)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            if (isWide) {
                // Side-by-side layout for Wide
                Row(
                    modifier = GlanceModifier
                        .padding(vertical = verticalPadding, horizontal = 16.dp)
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
                        alignEnd = false // Align start/center for row layout
                    )
                    Spacer(modifier = GlanceModifier.width(16.dp))
                    WidgetFooter(
                        context,
                        isFasting = fastingData.isFasting,
                        hours = hours,
                        isCompact = false,
                        isWide = true
                    )
                }
            } else {
                // Vertical layout for Compact and Medium/Expanded
                Column(
                    modifier = GlanceModifier
                        .padding(vertical = verticalPadding)
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
                        alignEnd = true // Original behavior
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    WidgetFooter(
                        context,
                        isFasting = fastingData.isFasting,
                        hours = hours,
                        isCompact = isCompact,
                        isWide = false
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

    val trackComposeColor = GlanceTheme.colors.surfaceVariant.getColor(context)
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
    isCompact: Boolean,
    isWide: Boolean
) {
    val actualHours = hours.coerceAtLeast(0)

    val largeTextSize = if (isCompact) 36.sp else if (isWide) 48.sp else 60.sp
    val smallTextSize = if (isCompact) 14.sp else if (isWide) 16.sp else 20.sp
    val goalMetTextSize = if (isCompact) 20.sp else 30.sp

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

    return Column {
        Text(
            text = context.getString(R.string.widget_not_fasting),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = if (isCompact) 18.sp else 24.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

val widgetMockFastingData = FastingDataItem(
    fastingGoalId = PredefinedFastingGoals.SIXTEEN_EIGHT.id,
    isFasting = true,
    startTimeInMillis = System.currentTimeMillis() - (12 * 60 * 60 * 1000L) // 12 hours ago
)

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 120, heightDp = 120) // Compact
@Preview(widthDp = 300, heightDp = 120) // Wide (New)
@Preview(widthDp = 300, heightDp = 200) // Medium
@Preview(widthDp = 300, heightDp = 300) // Expanded
@Composable
private fun OneWidgetContentPreview() {
    OneWidgetContent(
        fastingData = widgetMockFastingData,
        context = LocalContext.current
    )
}
