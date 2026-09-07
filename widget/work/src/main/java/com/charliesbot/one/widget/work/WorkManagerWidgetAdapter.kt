package com.charliesbot.one.widget.work

import android.content.Context
import androidx.concurrent.futures.await
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.charliesbot.one.widget.common.WidgetHost
import com.charliesbot.one.widget.common.WidgetPlatformAdapter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

class WorkManagerWidgetAdapter(
  context: Context,
  private val workName: String,
  private val host: WidgetHost,
  private val workManager: WorkManager = WorkManager.getInstance(context),
) : WidgetPlatformAdapter {
  override suspend fun hasActiveWidgets(): Boolean = host.hasActiveWidgets()

  override suspend fun hasActiveWork(): Boolean =
    try {
      val workInfos = workManager.getWorkInfosForUniqueWork(workName).await()
      workInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      false
    }

  override fun enqueueImmediateWork() {
    val workRequest = OneTimeWorkRequestBuilder<WidgetRefreshWorker>().addTag(workName).build()

    workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.KEEP, workRequest)
  }

  override fun enqueueDelayedWork(delayMillis: Long, replaceExisting: Boolean) {
    val policy = if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
    val workRequest =
      OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        .addTag(workName)
        .build()

    workManager.enqueueUniqueWork(workName, policy, workRequest)
  }

  override fun cancelScheduledWork() {
    workManager.cancelUniqueWork(workName)
  }

  override suspend fun requestWidgetUpdate() = host.requestWidgetUpdate()

  override fun canEnqueueImmediateRecovery(): Boolean = host.canEnqueueImmediateRecovery()
}
