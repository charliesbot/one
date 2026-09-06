package com.charliesbot.one.widget.wear

import android.content.Context
import androidx.glance.wear.GlanceWearWidgetManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.charliesbot.one.widget.common.WidgetRefreshCalculator
import java.util.concurrent.TimeUnit

class WearWidgetRefreshScheduler(
  private val context: Context,
  private val workManager: WorkManager = WorkManager.getInstance(context),
  private val activeWidgetChecker: suspend () -> Boolean = {
    GlanceWearWidgetManager(context).fetchActiveWidgets(OneWearWidget::class).isNotEmpty()
  },
) {
  companion object {
    const val WORK_NAME = "fasting_wear_widget_hourly_refresh"
  }

  suspend fun scheduleNext(
    startTimeMillis: Long,
    goalDurationMillis: Long,
    currentTimeMillis: Long = System.currentTimeMillis(),
  ) {
    if (!activeWidgetChecker()) {
      cancel()
      return
    }

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
      OneTimeWorkRequestBuilder<WearWidgetRefreshWorker>()
        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        .addTag(WORK_NAME)
        .build()

    workManager.enqueueUniqueWork(
      WORK_NAME,
      ExistingWorkPolicy.REPLACE,
      workRequest,
    )
  }

  fun cancel() {
    workManager.cancelUniqueWork(WORK_NAME)
  }
}
