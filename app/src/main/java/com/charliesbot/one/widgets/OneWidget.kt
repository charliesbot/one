package com.charliesbot.one.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal val TAG = "OneWidget"

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

class OneWidget() : GlanceAppWidget(), KoinComponent {
    private val fastingDataRepository: FastingDataRepository by inject()

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            OneWidgetSize.SMALL_SQUARE,
            OneWidgetSize.HORIZONTAL_RECTANGLE,
            OneWidgetSize.BIG_SQUARE
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val fastingData by fastingDataRepository.fastingDataItem.collectAsState(
                initial = FastingDataItem(
                    fastingGoalId = PredefinedFastingGoals.SIXTEEN_EIGHT.id
                )
            )

            OneWidgetContent(
                fastingData = fastingData,
                context = context
            )
        }
    }
}


