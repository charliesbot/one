package com.charliesbot.one.widget.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.charliesbot.one.widget.common.WidgetRefreshScheduler
import kotlinx.coroutines.CancellationException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WidgetRefreshWorker(context: Context, workerParameters: WorkerParameters) :
  CoroutineWorker(context, workerParameters), KoinComponent {
  private val scheduler: WidgetRefreshScheduler by inject()

  override suspend fun doWork(): Result =
    try {
      scheduler.refresh()
      Result.success()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Widget refresh worker failed (attempt $runAttemptCount)", e)
      if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
    }

  companion object {
    private const val TAG = "WidgetRefreshWorker"
    private const val MAX_RETRIES = 3
  }
}
