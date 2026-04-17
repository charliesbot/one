package com.charliesbot.onewearos.tiles

import android.content.Context
import android.util.Log
import com.charliesbot.shared.core.constants.AppConstants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class TileUpdateManager(
  private val applicationContext: Context,
  private val timeWindow: Long = 1000L,
  coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
  private val updater: (() -> Unit)? = null,
) {
  private val scope = CoroutineScope(SupervisorJob() + coroutineDispatcher)

  private val requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

  init {
    Log.d(AppConstants.LOG_TAG, "TileUpdateManager: Initializing and starting debounce collector.")
    startDebounceCollector()
  }

  @OptIn(FlowPreview::class)
  private fun startDebounceCollector() {
    scope.launch {
      requests.debounce(timeWindow).collect {
        if (updater != null) {
          updater.invoke()
        } else {
          performUpdate()
        }
      }
    }
  }

  private fun performUpdate() {
    runCatching {
        androidx.wear.tiles.TileService.getUpdater(applicationContext)
          .requestUpdate(FastingTileService::class.java)
      }
      .onFailure {
        Log.e(AppConstants.LOG_TAG, "TileUpdateManager: Error requesting tile update", it)
      }
  }

  fun requestUpdate() = requests.tryEmit(Unit)

  fun cancel() = scope.cancel()
}
