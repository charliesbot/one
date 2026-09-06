package com.charliesbot.one.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.charliesbot.one.widget.common.WidgetRefreshCalculator
import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.utils.GoalResolver
import java.util.concurrent.TimeUnit

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
) {
  companion object {
    const val WORK_NAME = "fasting_phone_widget_hourly_refresh"
  }

  suspend fun onFastingStartedOrUpdated(fastingData: FastingDataItem) {
    if (!activeWidgetChecker() || !fastingData.isFasting) {
      cancel()
      return
    }
    val goalDuration = goalResolver.resolveGoalDurationMillis(fastingData.fastingGoalId)
    schedule(
      startTimeMillis = fastingData.startTimeInMillis,
      goalDurationMillis = goalDuration,
      policy = ExistingWorkPolicy.REPLACE,
    )
  }

  fun onFastingCompleted() {
    cancel()
  }

  suspend fun reconcile() {
    if (!activeWidgetChecker()) {
      cancel()
      return
    }

    val current = fastingDataRepository.getCurrentFasting()
    if (current == null || !current.isFasting) {
      cancel()
      return
    }

    val workInfos = try {
      workManager.getWorkInfosForUniqueWork(WORK_NAME).get()
    } catch (e: Exception) {
      emptyList<WorkInfo>()
    }

    val hasActiveWork =
      workInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
    if (hasActiveWork) {
      // Preserve existing active work - do not cancel or postpone due refresh
      return
    }

    val goalDuration = goalResolver.resolveGoalDurationMillis(current.fastingGoalId)
    schedule(
      startTimeMillis = current.startTimeInMillis,
      goalDurationMillis = goalDuration,
      policy = ExistingWorkPolicy.KEEP,
    )
  }

  suspend fun onWorkerTickCompleted(
    snapshotStartTime: Long,
    snapshotGoalId: String,
    goalDurationMillis: Long,
    currentTimeMillis: Long = System.currentTimeMillis(),
  ) {
    val current = fastingDataRepository.getCurrentFasting()
    if (current == null || !current.isFasting) {
      cancel()
      return
    }

    // Protect against stale work: do not overwrite newer schedule if start time or goal changed
    if (current.startTimeInMillis != snapshotStartTime || current.fastingGoalId != snapshotGoalId) {
      return
    }

    val elapsed = (currentTimeMillis - current.startTimeInMillis).coerceAtLeast(0L)
    if (elapsed >= goalDurationMillis) {
      // Goal reached, end refresh chain
      cancel()
      return
    }

    schedule(
      startTimeMillis = current.startTimeInMillis,
      goalDurationMillis = goalDurationMillis,
      currentTimeMillis = currentTimeMillis,
      policy = ExistingWorkPolicy.REPLACE,
    )
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

    workManager.enqueueUniqueWork(
      WORK_NAME,
      policy,
      workRequest,
    )
  }

  fun cancel() {
    workManager.cancelUniqueWork(WORK_NAME)
  }
}
