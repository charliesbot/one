package com.charliesbot.shared.core.services

import com.charliesbot.shared.core.models.FastingDataItem

/**
 * Events triggered when the state of a fast changes.
 *
 * On Wear OS, this might update a complication.
 * On mobile, this might update a widget.
 */
interface FastingEventCallbacks {
    suspend fun onFastingStarted(fastingDataItem: FastingDataItem)
    suspend fun onFastingCompleted(fastingDataItem: FastingDataItem)

    suspend fun onFastingUpdated(fastingDataItem: FastingDataItem)
}