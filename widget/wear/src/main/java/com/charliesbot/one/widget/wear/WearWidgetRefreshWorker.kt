package com.charliesbot.one.widget.wear

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.utils.GoalResolver
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WearWidgetRefreshWorker @JvmOverloads constructor(
  context: Context,
  workerParameters: WorkerParameters,
  private val widgetUpdater: suspend (Context) -> Unit = { updateOneWearWidgets(it) },
) : CoroutineWorker(context, workerParameters), KoinComponent {

  private val fastingDataRepository: FastingDataRepository by inject()
  private val goalResolver: GoalResolver by inject()
  private val scheduler: WearWidgetRefreshScheduler by inject()

  override suspend fun doWork(): Result {
    val fastingData = fastingDataRepository.getCurrentFasting()
    if (fastingData == null || !fastingData.isFasting) {
      scheduler.cancel()
      widgetUpdater(applicationContext)
      return Result.success()
    }

    val goalDuration = goalResolver.resolveGoalDurationMillis(fastingData.fastingGoalId)
    val elapsed = (System.currentTimeMillis() - fastingData.startTimeInMillis).coerceAtLeast(0L)

    // Directly await Glance Wear widget recomposition
    widgetUpdater(applicationContext)

    if (elapsed < goalDuration) {
      scheduler.scheduleNext(
        startTimeMillis = fastingData.startTimeInMillis,
        goalDurationMillis = goalDuration,
      )
    } else {
      scheduler.cancel()
    }

    return Result.success()
  }
}
