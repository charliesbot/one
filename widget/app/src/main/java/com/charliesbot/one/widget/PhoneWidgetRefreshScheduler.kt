package com.charliesbot.one.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.concurrent.futures.await
import androidx.glance.appwidget.updateAll
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

class PhoneWidgetRefreshScheduler(
  private val context: Context,
  private val fastingDataRepository: FastingDataRepository,
  private val goalResolver: GoalResolver,
  private val workManager: WorkManager = WorkManager.getInstance(context),
  private val activeWidgetChecker: () -> Boolean = {
    AppWidgetManager.getInstance(context)
      .getAppWidgetIds(ComponentName(context, OneWidgetReceiver::class.java))
      .isNotEmpty()
  },
  private val widgetUpdater: suspend (Context) -> Unit = { OneWidget().updateAll(it) },
) {
  companion object {
    const val WORK_NAME = "fasting_phone_widget_hourly_refresh"
  }

  internal val mutex = Mutex()

  suspend fun onFastingStartedOrUpdated(fastingData: FastingDataItem) =
    mutex.withLock {
      if (!activeWidgetChecker() || !fastingData.isFasting) {
        cancel()
        return@withLock
      }
      val goalDuration = goalResolver.durationMillis(fastingData.fastingGoalId)
      schedule(
        startTimeMillis = fastingData.startTimeInMillis,
        goalDurationMillis = goalDuration,
        policy = ExistingWorkPolicy.REPLACE,
      )
    }

  suspend fun onFastingCompleted() = mutex.withLock { cancel() }

  suspend fun reconcile(currentTimeMillis: Long = System.currentTimeMillis()) =
    mutex.withLock {
      if (!activeWidgetChecker()) {
        cancel()
        return@withLock
      }

      val current = fastingDataRepository.getCurrentFasting()
      if (current == null || !current.isFasting) {
        cancel()
        return@withLock
      }

      val workInfos =
        try {
          workManager.getWorkInfosForUniqueWork(WORK_NAME).await()
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          emptyList<WorkInfo>()
        }

      val hasActiveWork =
        workInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
      if (hasActiveWork) {
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
        cancel()
        return@withLock
      }

      schedule(
        startTimeMillis = current.startTimeInMillis,
        goalDurationMillis = goalDuration,
        currentTimeMillis = currentTimeMillis,
        policy = ExistingWorkPolicy.KEEP,
      )
    }

  suspend fun onWorkerTickCompleted(
    snapshotStartTime: Long,
    snapshotGoalId: String,
    goalDurationMillis: Long,
    currentTimeMillis: Long = System.currentTimeMillis(),
  ) =
    mutex.withLock {
      if (!activeWidgetChecker()) {
        cancel()
        return@withLock
      }

      val current = fastingDataRepository.getCurrentFasting()
      if (current == null || !current.isFasting) {
        cancel()
        return@withLock
      }

      // Protect against stale work: do not overwrite newer schedule if start time or goal changed
      if (
        current.startTimeInMillis != snapshotStartTime || current.fastingGoalId != snapshotGoalId
      ) {
        return@withLock
      }

      val elapsed = (currentTimeMillis - current.startTimeInMillis).coerceAtLeast(0L)
      if (elapsed >= goalDurationMillis) {
        // Goal reached, end refresh chain
        cancel()
        return@withLock
      }

      schedule(
        startTimeMillis = current.startTimeInMillis,
        goalDurationMillis = goalDurationMillis,
        currentTimeMillis = currentTimeMillis,
        policy = ExistingWorkPolicy.REPLACE,
      )
    }

  fun enqueueImmediateRecovery(policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP) {
    if (!activeWidgetChecker()) {
      cancel()
      return
    }

    val workRequest =
      OneTimeWorkRequestBuilder<PhoneWidgetRefreshWorker>().addTag(WORK_NAME).build()

    workManager.enqueueUniqueWork(WORK_NAME, policy, workRequest)
  }

  private fun schedule(
    startTimeMillis: Long,
    goalDurationMillis: Long,
    currentTimeMillis: Long = System.currentTimeMillis(),
    policy: ExistingWorkPolicy,
  ) {
    val delayMillis =
      WidgetRefreshCalculator.calculateNextRefreshDelayMillis(
        currentTimeMillis = currentTimeMillis,
        startTimeMillis = startTimeMillis,
        goalDurationMillis = goalDurationMillis,
      )

    if (delayMillis == null || delayMillis <= 0L) {
      cancel()
      return
    }

    val workRequest =
      OneTimeWorkRequestBuilder<PhoneWidgetRefreshWorker>()
        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        .addTag(WORK_NAME)
        .build()

    workManager.enqueueUniqueWork(WORK_NAME, policy, workRequest)
  }

  fun cancel() {
    workManager.cancelUniqueWork(WORK_NAME)
  }
}
