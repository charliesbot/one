package com.charliesbot.one.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.utils.GoalResolver
import kotlinx.coroutines.CancellationException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PhoneWidgetRefreshWorker
@JvmOverloads
constructor(
  context: Context,
  workerParameters: WorkerParameters,
  private val widgetUpdater: suspend (Context) -> Unit = { OneWidget().updateAll(it) },
) : CoroutineWorker(context, workerParameters), KoinComponent {

  private val fastingDataRepository: FastingDataRepository by inject()
  private val goalResolver: GoalResolver by inject()
  private val scheduler: PhoneWidgetRefreshScheduler by inject()

  override suspend fun doWork(): Result =
    try {
      val snapshot = fastingDataRepository.getCurrentFasting()
      if (snapshot == null || !snapshot.isFasting) {
        scheduler.onFastingCompleted()
        widgetUpdater(applicationContext)
        Result.success()
      } else {
        val goalDuration = goalResolver.durationMillis(snapshot.fastingGoalId)

        // Directly await Glance widget recomposition
        widgetUpdater(applicationContext)

        // Delegate next boundary scheduling with snapshot verification against stale work
        scheduler.onWorkerTickCompleted(
          snapshotStartTime = snapshot.startTimeInMillis,
          snapshotGoalId = snapshot.fastingGoalId,
          goalDurationMillis = goalDuration,
        )

        Result.success()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Phone widget refresh worker failed (attempt $runAttemptCount)", e)
      if (runAttemptCount < MAX_RETRIES) {
        Result.retry()
      } else {
        Result.failure()
      }
    }

  companion object {
    private const val TAG = "PhoneWidgetWorker"
    private const val MAX_RETRIES = 3
  }
}
