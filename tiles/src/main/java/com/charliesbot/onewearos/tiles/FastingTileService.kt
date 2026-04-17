package com.charliesbot.onewearos.tiles

import android.util.Log
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.utils.FastingProgressUtil
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FastingTileService : TileService(), KoinComponent {

  private val repository: FastingDataRepository by inject()

  override fun onTileRequest(
    request: RequestBuilders.TileRequest
  ): ListenableFuture<TileBuilders.Tile> {
    Log.d(LOG_TAG, "FastingTileService: tileRequest")

    val tile = runBlocking {
      val fastingData = repository.fastingDataItem.first()

      val fastingProgress =
        if (fastingData.isFasting) {
          FastingProgressUtil.calculateFastingProgress(
            fastingData,
            currentTimeMillis = System.currentTimeMillis(),
          )
        } else {
          null
        }

      val fastingGoal =
        if (fastingData.isFasting) {
          PredefinedFastingGoals.getGoalById(fastingData.fastingGoalId)
        } else {
          null
        }

      val layout =
        FastingTileRenderer.renderTile(
          context = this@FastingTileService,
          fastingDataItem = fastingData,
          fastingProgress = fastingProgress,
          fastingGoal = fastingGoal,
        )

      TileBuilders.Tile.Builder()
        .setResourcesVersion("1")
        .setTileTimeline(
          TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(
              TimelineBuilders.TimelineEntry.Builder()
                .setLayout(LayoutElementBuilders.Layout.Builder().setRoot(layout).build())
                .build()
            )
            .build()
        )
        .build()
    }

    return ImmediateFuture(tile)
  }

  override fun onTileResourcesRequest(
    request: RequestBuilders.ResourcesRequest
  ): ListenableFuture<ResourceBuilders.Resources> {
    return ImmediateFuture(ResourceBuilders.Resources.Builder().setVersion("1").build())
  }
}

private class ImmediateFuture<V>(private val value: V) : ListenableFuture<V> {
  override fun addListener(listener: Runnable, executor: Executor) = executor.execute(listener)

  override fun cancel(mayInterruptIfRunning: Boolean) = false

  override fun isCancelled() = false

  override fun isDone() = true

  override fun get(): V = value

  override fun get(timeout: Long, unit: TimeUnit): V = value
}
