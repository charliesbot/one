package com.charliesbot.onewearos.tile

import android.content.Context
import android.util.Log
import androidx.wear.tiles.TileService
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
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val scope = CoroutineScope(SupervisorJob() + coroutineDispatcher)

    // "Fire and forget" bus. extraBufferCapacity = 1 keeps only the latest signal
    // if multiple tryEmit calls happen before debounce processes them.
    private val requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        Log.d(
            AppConstants.LOG_TAG,
            "TileUpdateManager: Initializing and starting debounce collector."
        )
        startDebounceCollector()
    }

    @OptIn(FlowPreview::class)
    private fun startDebounceCollector() {
        scope.launch {
            requests.debounce(timeWindow).collect {
                runCatching {
                    TileService.getUpdater(applicationContext)
                        .requestUpdate(FastingTileService::class.java)
                }.onFailure {
                    Log.e(AppConstants.LOG_TAG, "TileUpdateManager: Error requesting update", it)
                }
            }
        }
    }

    fun requestUpdate() = requests.tryEmit(Unit)

    fun cancel() = scope.cancel()
}
