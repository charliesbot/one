package com.charliesbot.onewearos.complication

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.GoalProgressComplicationData
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.charliesbot.onewearos.R
import com.charliesbot.onewearos.presentation.MainActivity
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.constants.FastGoal
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.utils.FastingProgress
import com.charliesbot.shared.core.utils.FastingProgressUtil
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


const val MOCKED_TARGET_HOURS = 16f

class MainComplicationService :
    SuspendingComplicationDataSourceService(), KoinComponent {

    private val repository: FastingDataRepository by inject()

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val icon = MonochromaticImage.Builder(
            Icon.createWithResource(this, R.drawable.ic_notification_status)
        ).build()
        val contentDescription =
            PlainComplicationText.Builder(getString(R.string.cd_fasting_preview)).build()
        val title =
            PlainComplicationText.Builder(getString(R.string.complication_title_hours_format, 8))
                .build()

        return when (type) {
            ComplicationType.GOAL_PROGRESS -> GoalProgressComplicationData.Builder(
                value = MOCKED_TARGET_HOURS / 2f, // Example: 8 hours towards 16h goal
                targetValue = MOCKED_TARGET_HOURS,
                contentDescription = contentDescription
            )
                .setMonochromaticImage(icon)
                .setTitle(title)
                .build()

            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = title,
                contentDescription = contentDescription
            )
                .setMonochromaticImage(icon)
                .setTitle(title)
                .build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder("8h / ${MOCKED_TARGET_HOURS.toInt()}h").build(),
                contentDescription = contentDescription
            )
                .setTitle(title)
                .setMonochromaticImage(icon)
                .build()

            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = MOCKED_TARGET_HOURS / 2f,
                min = 0f,
                max = MOCKED_TARGET_HOURS,
                contentDescription = contentDescription
            )
                .setTitle(title)
                .setMonochromaticImage(icon)
                .build()

            ComplicationType.MONOCHROMATIC_IMAGE -> MonochromaticImageComplicationData.Builder(
                monochromaticImage = icon,
                contentDescription = contentDescription
            ).build()

            else -> null
        }
    }


    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val fastingData = repository.fastingDataItem.first()
        Log.d(LOG_TAG, "Complication update request for type ${request.complicationType}")
        val tapAction = createTapIntent()

        return if (fastingData.isFasting) {
            createFastingComplicationData(request.complicationType, fastingData, tapAction)
        } else {
            createNotFastingComplicationData(request.complicationType, tapAction)
        }
    }

    private fun createNotFastingComplicationData(
        type: ComplicationType,
        tapAction: PendingIntent?
    ): ComplicationData? {
        val contentDescription =
            PlainComplicationText.Builder(getString(R.string.complication_text_not_fasting))
                .build()
        val icon = MonochromaticImage.Builder(
            Icon.createWithResource(this, R.drawable.ic_notification_status)
        ).build()

        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder("--").build(),
                contentDescription = contentDescription
            )
                .setMonochromaticImage(icon)
                .setTapAction(tapAction)
                .build()

            ComplicationType.GOAL_PROGRESS -> GoalProgressComplicationData.Builder(
                value = 0f,
                targetValue = 1f, // Avoid division by zero
                contentDescription = contentDescription
            )
                .setMonochromaticImage(icon)
                .setTapAction(tapAction)
                .build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder(getString(R.string.complication_text_not_fasting))
                    .build(),
                contentDescription = contentDescription
            )
                .setMonochromaticImage(icon)
                .setTapAction(tapAction)
                .build()

            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = 0f,
                min = 0f,
                max = 1f,
                contentDescription = contentDescription
            )
                .setMonochromaticImage(icon)
                .setTapAction(tapAction)
                .build()

            ComplicationType.MONOCHROMATIC_IMAGE -> MonochromaticImageComplicationData.Builder(
                monochromaticImage = icon,
                contentDescription = contentDescription
            )
                .setTapAction(tapAction)
                .build()

            else -> null
        }
    }

    private fun createFastingComplicationData(
        type: ComplicationType,
        fastingData: FastingDataItem,
        tapAction: PendingIntent?
    ): ComplicationData? {
        val fastingProgress = FastingProgressUtil.calculateFastingProgress(
            fastingData,
            currentTimeMillis = System.currentTimeMillis()
        )
        val fastingGoal = PredefinedFastingGoals.getGoalById(fastingData.fastingGoalId)
        val contentDescription = createContentDescription(fastingProgress, fastingGoal)
        val icon = MonochromaticImage.Builder(
            Icon.createWithResource(this, R.drawable.ic_notification_status)
        ).build()
        val elapsedHours = fastingProgress.elapsedHours.toInt()
        val title =
            PlainComplicationText.Builder(getString(R.string.complication_title_hours_format, elapsedHours))
                .build()

        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder("${elapsedHours}h").build(),
                contentDescription = contentDescription
            )
                .setTitle(PlainComplicationText.Builder(fastingGoal.durationDisplay).build())
                .setMonochromaticImage(icon)
                .setTapAction(tapAction)
                .build()

            ComplicationType.GOAL_PROGRESS -> GoalProgressComplicationData.Builder(
                value = fastingProgress.elapsedHours.toFloat()
                    .coerceAtMost(fastingProgress.targetHours.toFloat()),
                targetValue = fastingProgress.targetHours.toFloat(),
                contentDescription = contentDescription
            )
                .setTitle(title)
                .setMonochromaticImage(icon)
                .setTapAction(tapAction)
                .build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder("${elapsedHours}h / ${fastingGoal.durationDisplay}").build(),
                contentDescription = contentDescription
            )
                .setTitle(title)
                .setMonochromaticImage(icon)
                .setTapAction(tapAction)
                .build()

            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = fastingProgress.elapsedHours.toFloat()
                    .coerceAtMost(fastingProgress.targetHours.toFloat()),
                min = 0f,
                max = fastingProgress.targetHours.toFloat(),
                contentDescription = contentDescription
            )
                .setTitle(title)
                .setMonochromaticImage(icon)
                .setTapAction(tapAction)
                .build()

            ComplicationType.MONOCHROMATIC_IMAGE -> MonochromaticImageComplicationData.Builder(
                monochromaticImage = icon,
                contentDescription = contentDescription
            )
                .setTapAction(tapAction)
                .build()

            else -> null
        }
    }

    private fun createContentDescription(
        fastingProgress: FastingProgress,
        fastingGoal: FastGoal
    ) = PlainComplicationText.Builder(
        getString(
            R.string.complication_text_fasting_format,
            fastingProgress.progressPercentage,
            fastingProgress.elapsedHours.toInt().toString(),
            getString(R.string.target_duration_short, fastingGoal.durationDisplay)
        )
    ).build()

    private fun createTapIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}