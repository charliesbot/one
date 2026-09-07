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
import com.charliesbot.one.widget.common.WidgetPlatformAdapter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

class PhoneWidgetPlatformAdapter(
  private val context: Context,
  private val workManager: WorkManager = WorkManager.getInstance(context),
  private val activeWidgetChecker: () -> Boolean = {
    AppWidgetManager.getInstance(context)
      .getAppWidgetIds(ComponentName(context, OneWidgetReceiver::class.java))
      .isNotEmpty()
  },
  private val widgetUpdater: suspend (Context) -> Unit = { OneWidget().updateAll(it) },
) : WidgetPlatformAdapter {

  companion object {
    const val WORK_NAME = "fasting_phone_widget_hourly_refresh"
  }

  override suspend fun hasActiveWidgets(): Boolean = activeWidgetChecker()

  override suspend fun hasActiveWork(): Boolean =
    try {
      val workInfos = workManager.getWorkInfosForUniqueWork(WORK_NAME).await()
      workInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      false
    }

  override fun enqueueImmediateWork() {
    val workRequest =
      OneTimeWorkRequestBuilder<PhoneWidgetRefreshWorker>().addTag(WORK_NAME).build()

    workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, workRequest)
  }

  override fun enqueueDelayedWork(delayMillis: Long, replaceExisting: Boolean) {
    val policy = if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
    val workRequest =
      OneTimeWorkRequestBuilder<PhoneWidgetRefreshWorker>()
        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        .addTag(WORK_NAME)
        .build()

    workManager.enqueueUniqueWork(WORK_NAME, policy, workRequest)
  }

  override fun cancelScheduledWork() {
    workManager.cancelUniqueWork(WORK_NAME)
  }

  override suspend fun requestWidgetUpdate() {
    widgetUpdater(context)
  }

  override fun canEnqueueImmediateRecovery(): Boolean = activeWidgetChecker()
}
