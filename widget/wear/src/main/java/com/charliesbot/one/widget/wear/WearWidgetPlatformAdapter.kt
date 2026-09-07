package com.charliesbot.one.widget.wear

import android.content.Context
import androidx.concurrent.futures.await
import androidx.glance.wear.GlanceWearWidgetManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.charliesbot.one.widget.common.WidgetPlatformAdapter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

class WearWidgetPlatformAdapter(
  private val context: Context,
  private val workManager: WorkManager = WorkManager.getInstance(context),
  private val activeWidgetChecker: suspend () -> Boolean = {
    GlanceWearWidgetManager(context).fetchActiveWidgets(OneWearWidget::class).isNotEmpty()
  },
  private val widgetUpdater: suspend (Context) -> Unit = { updateOneWearWidgets(it) },
) : WidgetPlatformAdapter {

  companion object {
    const val WORK_NAME = "fasting_wear_widget_hourly_refresh"
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
    val workRequest = OneTimeWorkRequestBuilder<WearWidgetRefreshWorker>().addTag(WORK_NAME).build()

    workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, workRequest)
  }

  override fun enqueueDelayedWork(delayMillis: Long, replaceExisting: Boolean) {
    val policy = if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
    val workRequest =
      OneTimeWorkRequestBuilder<WearWidgetRefreshWorker>()
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

  override fun canEnqueueImmediateRecovery(): Boolean = true
}
