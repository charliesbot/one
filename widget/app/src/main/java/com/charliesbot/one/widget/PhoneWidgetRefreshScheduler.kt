package com.charliesbot.one.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.charliesbot.one.widget.common.WidgetRefreshCalculator
import java.util.concurrent.TimeUnit

class PhoneWidgetRefreshScheduler(
  private val context: Context,
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

  fun scheduleNext(
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
      OneTimeWorkRequestBuilder<PhoneWidgetRefreshWorker>()
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
