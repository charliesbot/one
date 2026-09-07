package com.charliesbot.one.widget.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.charliesbot.one.widget.common.WidgetHost
import com.charliesbot.one.widget.common.WidgetPlatformAdapter
import java.util.concurrent.TimeUnit

class WorkManagerWidgetAdapter(
  context: Context,
  private val workName: String,
  private val host: WidgetHost,
  private val workManager: WorkManager = WorkManager.getInstance(context),
) : WidgetPlatformAdapter {
  override suspend fun hasActiveWidgets(): Boolean = host.hasActiveWidgets()

  override fun ensurePeriodicWork() {
    val request =
      PeriodicWorkRequestBuilder<WidgetRefreshWorker>(1, TimeUnit.HOURS).addTag(workName).build()
    workManager.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.KEEP, request)
  }

  override fun cancelScheduledWork() {
    workManager.cancelUniqueWork(workName)
  }

  override suspend fun requestWidgetUpdate() = host.requestWidgetUpdate()

  override fun canRequestRefreshImmediately(): Boolean = host.canRequestRefreshImmediately()
}
