package com.charliesbot.one.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.provideContent
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
import com.charliesbot.shared.core.constants.AppConstants
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.utils.calculateProgressFraction
import com.charliesbot.shared.core.utils.getHours
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object ProgressBitmap {
    fun draw(
        progress: Float,
        sizePx: Int,
        strokePx: Float,
        indicator: Int = 0xFFFF6A4E.toInt(),
        track: Int = 0xFF2B2B2B.toInt(),
    ): Bitmap = createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).apply {
        val c = Canvas(this)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokePx
            strokeCap = Paint.Cap.ROUND
        }

        val radius = sizePx / 2f - strokePx / 2f
        val rect = RectF(
            strokePx / 2, strokePx / 2,
            sizePx - strokePx / 2, sizePx - strokePx / 2
        )

        // track
        paint.color = track; c.drawCircle(sizePx / 2f, sizePx / 2f, radius, paint)

        // progress
        paint.color = indicator
        c.drawArc(rect, -90f, progress.coerceIn(0f, 1f) * 360f, false, paint)
    }
}

class OneWidget(private val isPreview: Boolean = false) : GlanceAppWidget(), KoinComponent {
    private val ringDp: Dp = 75.dp
    private val strokeDp: Dp = 10.dp
    private val fastingDataRepository: FastingDataRepository by inject()

    override val sizeMode: SizeMode = SizeMode.Responsive(
        sizes = setOf(
            OneWidgetSize.COMPACT,
            OneWidgetSize.MEDIUM,
            OneWidgetSize.EXPANDED
        )
    )

    private val fakeFastingData = FastingDataItem(
        fastingGoalId = PredefinedFastingGoals.SIXTEEN_EIGHT.id,
        isFasting = true,
        startTimeInMillis = System.currentTimeMillis() - (12 * 60 * 60 * 1000L) // 12 hours ago
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val fastingData by if (isPreview) {
                remember { mutableStateOf(fakeFastingData) }
            } else {
                fastingDataRepository.fastingDataItem.collectAsState(
                    initial = FastingDataItem(
                        fastingGoalId = PredefinedFastingGoals.SIXTEEN_EIGHT.id
                    )
                )
            }
            val currentTime = System.currentTimeMillis()
            val startTimeInMillis = fastingData.startTimeInMillis
            val elapsedMillis = (currentTime - startTimeInMillis).coerceAtLeast(0)
            val selectedGoal = PredefinedFastingGoals.getGoalById(fastingData.fastingGoalId)
            val hours = getHours(selectedGoal.durationMillis) - getHours(elapsedMillis)
            val isGoalMet = fastingData.isFasting && hours <= 0

            if (!isPreview) {
                Log.d(
                    AppConstants.LOG_TAG,
                    "OneWidget: provideGlance INVOKED for id $id. CurrentTimeMillis: ${System.currentTimeMillis()}"
                )
                Log.d(
                    AppConstants.LOG_TAG,
                    "OneWidget: provideGlance - fastingData fetched for UI for id $id: $fastingData"
                )
            }

            Log.e("TEST TEST", "LocalSize: ${LocalSize.current} isPreview: $isPreview")

            Scaffold(
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.widgetBackground)
                    .clickable(
                        actionStartActivity<MainActivity>()
                    )
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
                        isPreview = isPreview
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    WidgetFooter(
                        isFasting = fastingData.isFasting,
                        hours = hours,
                        startTimeInMillis = startTimeInMillis,
                        context = context,
                    )
                }
            }
        }
    }

    @Composable
    fun WidgetFooter(
        context: Context,
        startTimeInMillis: Long,
        isFasting: Boolean, hours: Long
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

    @Composable
    fun WidgetProgressBar(
        isFasting: Boolean,
        elapsedTime: Long,
        fastingGoalMillis: Long,
        isGoalMet: Boolean,
        isPreview: Boolean = false
    ) {
        val context = LocalContext.current
        val displayMetrics = context.resources.displayMetrics
        val progress = calculateProgressFraction(elapsedTime, fastingGoalMillis)

        val trackComposeColor =
            GlanceTheme.colors.surfaceVariant.getColor(context)
        // If it is fasting, instead of hiding the progress
        // we can play a trick and just paint two circles with the same color
        val indicatorComposeColor = if (isGoalMet) {
            GlanceTheme.colors.tertiary.getColor(context)
        } else if (isFasting) {
            GlanceTheme.colors.primary.getColor(context)
        } else {
            trackComposeColor
        }

        // Use produceState to generate the bitmap on a background thread
        // It will re-run if 'progress', 'ringDp', 'strokeDp', 'indicatorComposeColor', or 'trackComposeColor' changes.
        val ringBitmap = if (isPreview) {
            return
        } else {
            val ringBitmap by produceState<Bitmap?>(
                initialValue = null,
                keys = arrayOf(progress, ringDp, strokeDp, indicatorComposeColor, trackComposeColor)
            ) {
                val sizePx = (ringDp.value * displayMetrics.density).toInt()
                val strokePx = (strokeDp.value * displayMetrics.density)

                value = withContext(Dispatchers.Default) {
                    ProgressBitmap.draw(
                        progress = progress,
                        sizePx = sizePx,
                        strokePx = strokePx,
                        indicator = indicatorComposeColor.toArgb(),
                        track = trackComposeColor.toArgb()
                    )
                }
            }
            ringBitmap
        } ?: return

        Row(
            horizontalAlignment = Alignment.End,
            modifier = GlanceModifier.fillMaxWidth()
        ) {
            Image(
                provider = ImageProvider(ringBitmap),
                contentDescription = context.getString(R.string.progress_ring_desc),
                modifier = GlanceModifier.size(ringDp) // Use the Dp value for Glance size
            )
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 300, heightDp = 200)
//    @Preview(widthDp = 120, heightDp = 115) // Min drop size
//    @Preview(widthDp = 140, heightDp = 140) // all buttons; center button transparent bg
//    @Preview(widthDp = 624, heightDp = 200) // Max size
@Composable
fun OneWidgetPreview() {
    GlanceTheme {
//        MyContent()
    }
}


