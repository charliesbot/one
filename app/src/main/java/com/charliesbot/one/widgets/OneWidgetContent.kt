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
    ringDp: Dp,
    strokeDp: Dp,
    context: Context
) {
    val currentTime = System.currentTimeMillis()
    val startTimeInMillis = fastingData.startTimeInMillis
    val elapsedMillis = (currentTime - startTimeInMillis).coerceAtLeast(0)
    val selectedGoal = PredefinedFastingGoals.getGoalById(fastingData.fastingGoalId)
    val hours = getHours(selectedGoal.durationMillis) - getHours(elapsedMillis)
    val isGoalMet = fastingData.isFasting && hours <= 0

    GlanceTheme {
        Scaffold(
            modifier = GlanceModifier
                .background(GlanceTheme.colors.widgetBackground)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Column(
                modifier = GlanceModifier
                    .padding(vertical = 24.dp)
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WidgetProgressBar(
                    isFasting = fastingData.isFasting,
                    elapsedTime = elapsedMillis,
                    fastingGoalMillis = selectedGoal.durationMillis,
                    isGoalMet = isGoalMet,
                    ringDp = ringDp,
                    strokeDp = strokeDp
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                WidgetFooter(
                    context,
                    isFasting = fastingData.isFasting,
                    hours = hours,
                )
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
    strokeDp: Dp
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
        horizontalAlignment = Alignment.End,
        modifier = GlanceModifier.fillMaxWidth()
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
) {
    val actualHours = hours.coerceAtLeast(0)
    if (isFasting && actualHours <= 0) {
        return Row {
            Text(
                text = context.getString(R.string.widget_goal_met_part_1),
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontSize = 30.sp,
                )
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = context.getString(R.string.widget_goal_met_part_2),
                style = TextStyle(
                    color = GlanceTheme.colors.onTertiaryContainer,
                    fontSize = 30.sp,
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
                    fontSize = 60.sp,
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
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    return Column {
        Text(
            text = context.getString(R.string.widget_not_fasting),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface, fontSize = 24.sp,
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
@Preview(widthDp = 120, heightDp = 120) // Min drop size
@Preview(widthDp = 140, heightDp = 140)
@Preview(widthDp = 300, heightDp = 200)
@Preview(widthDp = 400, heightDp = 200) // Max size
@Composable
private fun OneWidgetContentPreview() {
        OneWidgetContent(
            fastingData = widgetMockFastingData,
            ringDp = 75.dp,
            strokeDp = 10.dp,
            context = LocalContext.current
        )
}
