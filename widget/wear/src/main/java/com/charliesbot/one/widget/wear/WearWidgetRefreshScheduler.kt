package com.charliesbot.one.widget.wear

import android.content.Context
import androidx.concurrent.futures.await
import androidx.glance.wear.GlanceWearWidgetManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.charliesbot.one.widget.common.WidgetRefreshCalculator
import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.utils.GoalResolver
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WearWidgetRefreshScheduler(
  private val context: Context,
  private val fastingDataRepository: FastingDataRepository,
  private val goalResolver: GoalResolver,
  private val workManager: WorkManager = WorkManager.getInstance(context),
  private val activeWidgetChecker: suspend () -> Boolean = {
    GlanceWearWidgetManager(context).fetchActiveWidgets(OneWearWidget::class).isNotEmpty()
  },
  private val widgetUpdater: suspend (Context) -> Unit = { updateOneWearWidgets(it) },
) {
  companion object {
    const val WORK_NAME = "fasting_wear_widget_hourly_refresh"
  }

  private val mutex = Mutex()

  suspend fun onFastingStartedOrUpdated(
    fastingData: FastingDataItem,
    currentTimeMillis: Long = System.currentTimeMillis(),
  ) =
    mutex.withLock {
      if (!activeWidgetChecker() || !fastingData.isFasting) {
        cancelLocked()
        return@withLock
      }
      val goalDuration = goalResolver.durationMillis(fastingData.fastingGoalId)
      val delayMillis =
        WidgetRefreshCalculator.calculateNextRefreshDelayMillis(
          currentTimeMillis = currentTimeMillis,
          startTimeMillis = fastingData.startTimeInMillis,
          goalDurationMillis = goalDuration,
        )
      scheduleLocked(delayMillis = delayMillis, policy = ExistingWorkPolicy.REPLACE)
    }

  suspend fun onFastingCompleted() = cancel()

  suspend fun reconcile(currentTimeMillis: Long = System.currentTimeMillis()) =
    mutex.withLock {
      if (!activeWidgetChecker()) {
        cancelLocked()
        return@withLock
      }

      val current = fastingDataRepository.getCurrentFasting()
      if (current == null || !current.isFasting) {
        cancelLocked()
        return@withLock
      }

      if (hasActiveWork()) {
        // Preserve existing active work - do not cancel or postpone due refresh
        return@withLock
      }

      val goalDuration = goalResolver.durationMillis(current.fastingGoalId)
      val delayMillis =
        WidgetRefreshCalculator.calculateNextRefreshDelayMillis(
          currentTimeMillis = currentTimeMillis,
          startTimeMillis = current.startTimeInMillis,
          goalDurationMillis = goalDuration,
        )

      if (delayMillis == null || delayMillis <= 0L) {
        // Fast has reached/passed goal while work was missing; update widget immediately
        widgetUpdater(context)
        cancelLocked()
        return@withLock
      }

      scheduleLocked(delayMillis = delayMillis, policy = ExistingWorkPolicy.KEEP)
    }

  suspend fun onWorkerTickCompleted(
    snapshotStartTime: Long,
    snapshotGoalId: String,
    goalDurationMillis: Long,
    currentTimeMillis: Long = System.currentTimeMillis(),
  ) =
    mutex.withLock {
      if (!activeWidgetChecker()) {
        cancelLocked()
        return@withLock
      }

      val current = fastingDataRepository.getCurrentFasting()
      if (current == null || !current.isFasting) {
        cancelLocked()
        return@withLock
      }

      // Protect against stale work: do not overwrite newer schedule if start time or goal changed
      if (
        current.startTimeInMillis != snapshotStartTime || current.fastingGoalId != snapshotGoalId
      ) {
        return@withLock
      }

      val delayMillis =
        WidgetRefreshCalculator.calculateNextRefreshDelayMillis(
          currentTimeMillis = currentTimeMillis,
          startTimeMillis = current.startTimeInMillis,
          goalDurationMillis = goalDurationMillis,
        )

      scheduleLocked(delayMillis = delayMillis, policy = ExistingWorkPolicy.REPLACE)
    }

  suspend fun enqueueImmediateRecovery() =
    mutex.withLock {
      val workRequest =
        OneTimeWorkRequestBuilder<WearWidgetRefreshWorker>().addTag(WORK_NAME).build()

      workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, workRequest)
    }

  suspend fun cancel() = mutex.withLock { cancelLocked() }

  private fun cancelLocked() {
    workManager.cancelUniqueWork(WORK_NAME)
  }

  private suspend fun hasActiveWork(): Boolean =
    try {
      val workInfos = workManager.getWorkInfosForUniqueWork(WORK_NAME).await()
      workInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      false
    }

  private fun scheduleLocked(delayMillis: Long?, policy: ExistingWorkPolicy) {
    if (delayMillis == null || delayMillis <= 0L) {
      cancelLocked()
      return
    }

    val workRequest =
      OneTimeWorkRequestBuilder<WearWidgetRefreshWorker>()
        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        .addTag(WORK_NAME)
        .build()

    workManager.enqueueUniqueWork(WORK_NAME, policy, workRequest)
  }
}
