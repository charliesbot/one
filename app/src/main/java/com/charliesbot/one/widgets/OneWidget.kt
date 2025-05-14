package com.charliesbot.one.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import com.charliesbot.one.MainActivity
import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepository
import com.charliesbot.shared.core.utils.calculateProgressFraction
import com.charliesbot.shared.core.utils.getHours
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object ProgressBitmap {
    fun draw(
        progress: Float,
        sizePx: Int,
        strokePx: Float,
        indicator: Int = 0xFFFF6A4E.toInt(),
        track: Int = 0xFF2B2B2B.toInt()
    ): Bitmap = createBitmap(sizePx, sizePx, Bitmap.Config.RGBA_F16).apply {
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

class OneWidget : GlanceAppWidget(), KoinComponent {
    private val ringDp: Dp = 70.dp
    private val strokeDp: Dp = 10.dp
    private val fastingDataRepository: FastingDataRepository by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val fastingData = fastingDataRepository.getCurrentFasting()!!
        val currentTime = System.currentTimeMillis()
        val elapsedMillis = (currentTime - fastingData.startTimeInMillis).coerceAtLeast(0)

        provideContent {
            Scaffold(
                modifier = GlanceModifier.clickable(
                    actionStartActivity<MainActivity>()
                )
            ) {
                Column(
                    modifier = GlanceModifier
                        .padding(vertical = 24.dp)
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WidgetProgressBar(
                        isFasting = fastingData.isFasting,
                        elapsedTime = elapsedMillis
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    WidgetFooter(isFasting = fastingData.isFasting, elapsedMillis = elapsedMillis)
                }
            }
        }
    }

    @Composable
    fun WidgetFooter(isFasting: Boolean, elapsedMillis: Long) {
        if (isFasting) {
            Column() {
                Text(
                    text = (16 - getHours(elapsedMillis)).coerceAtLeast(0).toString(),
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 60.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "hours left!",
                    style = TextStyle(
                        color = GlanceTheme.colors.secondary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        } else {
            Text(
                text = "Time Since Last Fast",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "2 months, 15 days",
                style = TextStyle(
                    color = GlanceTheme.colors.secondary,
                    fontSize = 12.sp,
                )
            )
        }

    }

    @Composable
    fun WidgetProgressBar(
        isFasting: Boolean,
        elapsedTime: Long
    ) {
        val context = LocalContext.current
        val displayMetrics = context.resources.displayMetrics
        val progress = calculateProgressFraction(elapsedTime)

        // Resolve GlanceTheme colors *inside* the @Composable function
        val trackComposeColor =
            GlanceTheme.colors.surfaceVariant.getColor(context)
        // If it is fasting, instead of hiding the progress
        // we can play a trick and just paint two circles with the same color
        val indicatorComposeColor =
            if (isFasting) GlanceTheme.colors.primary.getColor(context) else trackComposeColor

        // Use produceState to generate the bitmap on a background thread
        // It will re-run if 'progress', 'ringDp', 'strokeDp', 'indicatorComposeColor', or 'trackComposeColor' changes.
        val ringBitmap by produceState<Bitmap?>(
            initialValue = null, // Initial value while bitmap is loading
            keys = arrayOf(
                progress,
                ringDp,
                strokeDp,
                indicatorComposeColor,
                trackComposeColor
            ) // Keys for recomposition
        ) {
            // Convert Dp to Px for drawing (can be done here or inside withContext)
            val sizePx = (ringDp.value * displayMetrics.density).toInt()
            val strokePx = (strokeDp.value * displayMetrics.density)

            // Generate bitmap in the background
            value = withContext(Dispatchers.Default) {
                ProgressBitmap.draw(
                    progress = progress,
                    sizePx = sizePx,
                    strokePx = strokePx,
                    indicator = indicatorComposeColor.toArgb(), // Convert Compose Color to Int ARGB
                    track = trackComposeColor.toArgb()         // Convert Compose Color to Int ARGB
                )
            }
        }

        ringBitmap?.let {
            Row(
                horizontalAlignment = Alignment.End,
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                Image(
                    provider = ImageProvider(it),
                    contentDescription = "Progress Ring", // TODO: More descriptive
                    modifier = GlanceModifier.size(ringDp) // Use the Dp value for Glance size
                )
            }
        }
    }

//    @OptIn(ExperimentalGlancePreviewApi::class)
//    @Preview(widthDp = 56, heightDp = 56) // Min resize size
//    @Preview(widthDp = 120, heightDp = 115) // Min drop size
//    @Preview(widthDp = 140, heightDp = 140) // all buttons; center button transparent bg
//    @Preview(widthDp = 624, heightDp = 200) // Max size
//    @Composable
//    fun OneWidgetPreview() {
//        GlanceTheme {
//            MyContent()
//        }
//    }
}

