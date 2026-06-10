package com.charliesbot.shared.core.updates

import android.util.Log
import com.charliesbot.shared.core.domain.constants.AppConstants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class DebouncedUpdateRequester(
  private val name: String,
  private val timeWindow: Long = 1000L,
  coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
  private val update: suspend () -> Unit,
  private val logInitialized: () -> Unit = {
    Log.d(AppConstants.LOG_TAG, "$name: Initializing and starting debounce collector.")
  },
  private val logUpdateFailure: (Throwable) -> Unit = {
    Log.e(AppConstants.LOG_TAG, "$name: Error while updating.", it)
  },
) {
  private val scope = CoroutineScope(SupervisorJob() + coroutineDispatcher)

  // "Fire and forget" bus. extraBufferCapacity = 1 keeps only the latest signal
  // if multiple tryEmit calls happen before debounce processes them.
  private val requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

  init {
    logInitialized()
    startDebounceCollector()
  }

  @OptIn(FlowPreview::class)
  private fun startDebounceCollector() {
    scope.launch {
      requests.debounce(timeWindow).collect { runCatching { update() }.onFailure(logUpdateFailure) }
    }
  }

  fun requestUpdate() = requests.tryEmit(Unit)

  fun cancel() = scope.cancel()
}
