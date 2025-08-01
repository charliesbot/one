package com.charliesbot.onewearos.complication

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.GoalProgressComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.charliesbot.onewearos.R
import com.charliesbot.onewearos.presentation.MainActivity
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.utils.FastingProgressUtil
import com.charliesbot.shared.core.utils.calculateProgressPercentage
import com.charliesbot.shared.core.utils.getHours
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


const val MOCKED_TARGET_HOURS = 16f

class MainComplicationService() :
    SuspendingComplicationDataSourceService(), KoinComponent {

    private val repository: FastingDataRepository by inject()

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.GOAL_PROGRESS) {
            return null
        }
        return GoalProgressComplicationData.Builder(
            value = MOCKED_TARGET_HOURS / 2f, // Example: 8 hours towards 16h goal
            targetValue = MOCKED_TARGET_HOURS,
            contentDescription = PlainComplicationText.Builder(getString(R.string.cd_fasting_preview))
                .build()
        )
            .setMonochromaticImage(
                MonochromaticImage.Builder(
                    Icon.createWithResource(
                        this,
                        R.drawable.ic_notification_status
                    )
                ).build()
            )
            .setTitle(
                PlainComplicationText.Builder(
                    getString(
                        R.string.complication_title_hours_format,
                        8
                    )
                ).build()
            ) // Example elapsed time
            .build()
    }


    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val fastingData = repository.fastingDataItem.first()
        Log.e(LOG_TAG, "fastingData: $fastingData")
        val tapAction = createTapIntent()
        if (!fastingData.isFasting) {
            return GoalProgressComplicationData.Builder(
                value = 0f,
                targetValue = 0f,
                contentDescription = PlainComplicationText.Builder(
                    getString(R.string.complication_text_not_fasting)
                ).build()
            )
                .setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(
                            this,
                            R.drawable.ic_notification_status
                        )
                    ).build()
                )
                .setTapAction(tapAction)
                .build()
        }

        return createIsFastingComplicationData(fastingData, tapAction)
    }

    private fun createIsFastingComplicationData(
        fastingData: FastingDataItem,
        tapAction: PendingIntent?
    ): ComplicationData {
        val fastingProgress = FastingProgressUtil.calculateFastingProgress(
            fastingData,
            currentTimeMillis = System.currentTimeMillis()
        )
        val fastingGoal = PredefinedFastingGoals.getGoalById(fastingData.fastingGoalId)

        return GoalProgressComplicationData.Builder(
            value = fastingProgress.elapsedHours.toFloat()
                .coerceAtMost(fastingProgress.targetHours.toFloat()),
            targetValue = fastingProgress.targetHours.toFloat(),
            contentDescription = PlainComplicationText.Builder(
                getString(
                    R.string.complication_text_fasting_format,
                    fastingProgress.progressPercentage,
                    fastingProgress.elapsedHours.toInt(),
                    getString(
                        R.string.target_duration_short,
                        fastingGoal.durationDisplay
                    )
                )
            ).build()
        )
            .setMonochromaticImage(
                MonochromaticImage.Builder(
                    Icon.createWithResource(this, R.drawable.ic_notification_status)
                ).build()
            )
            .setTitle(
                PlainComplicationText.Builder(
                    getString(
                        R.string.complication_title_hours_format,
                        fastingProgress.elapsedHours.toInt()
                    )
                ).build()
            )
            .setTapAction(tapAction)
            .build()

    }

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