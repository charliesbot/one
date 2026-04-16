package com.charliesbot.onewearos.tiles

import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture

/** Tile service for displaying fasting status. Full implementation in a later step. */
class FastingTileService : TileService() {
  override fun onTileRequest(
    request: RequestBuilders.TileRequest
  ): ListenableFuture<TileBuilders.Tile> {
    throw UnsupportedOperationException("Not yet implemented")
  }

  override fun onTileResourcesRequest(
    request: RequestBuilders.ResourcesRequest
  ): ListenableFuture<ResourceBuilders.Resources> {
    throw UnsupportedOperationException("Not yet implemented")
  }
}
