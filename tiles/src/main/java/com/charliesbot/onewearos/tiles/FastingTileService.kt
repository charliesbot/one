package com.charliesbot.onewearos.tiles

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FastingTileService : TileService(), KoinComponent {

  private val repository: FastingDataRepository by inject()

  override fun onTileRequest(
    request: RequestBuilders.TileRequest
  ): ListenableFuture<TileBuilders.Tile> {
    Log.d(LOG_TAG, "FastingTileService: onTileRequest")

    val fastingData = runBlocking { repository.fastingDataItem.first() }

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

    val tile =
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

    return ImmediateFuture(tile)
  }

  override fun onTileResourcesRequest(
    request: RequestBuilders.ResourcesRequest
  ): ListenableFuture<ResourceBuilders.Resources> {
    val resources = ResourceBuilders.Resources.Builder().setVersion("1").build()
    return ImmediateFuture(resources)
  }

  private fun createTapIntent(): PendingIntent {
    val intent =
      Intent().apply {
        component =
          ComponentName(
            "com.charliesbot.one",
            "com.charliesbot.onewearos.presentation.MainActivity",
          )
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }

    return PendingIntent.getActivity(
      this,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private class ImmediateFuture<T>(private val value: T) : ListenableFuture<T> {
    override fun cancel(mayInterruptIfRunning: Boolean) = false

    override fun isCancelled() = false

    override fun isDone() = true

    override fun get(): T = value

    override fun get(timeout: Long, unit: java.util.concurrent.TimeUnit): T = value

    override fun addListener(listener: Runnable, executor: java.util.concurrent.Executor) {
      executor.execute(listener)
    }
  }
}
