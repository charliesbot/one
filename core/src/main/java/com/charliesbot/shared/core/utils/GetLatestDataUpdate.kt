package com.charliesbot.shared.core.utils

import com.charliesbot.shared.core.constants.DataLayerConstants
import com.charliesbot.shared.core.models.FastingDataItem
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem

// Data Layer can have multiple entries for the same path.
// We need to find the most recent one, otherwise we might overwrite state
// before it's committed.
fun getLatestFastingState(
    dataEvents: DataEventBuffer,
): FastingDataItem? {
    var newestItem: FastingDataItem? = null
    var mostRecentTimestamp = Long.MIN_VALUE

    for (event in dataEvents) {
        if (event.type != DataEvent.TYPE_CHANGED ||
            event.dataItem.uri.path != DataLayerConstants.FASTING_PATH
        ) {
            continue
        }
        val currentItem =
            getFastingItemFromDataLayer(DataMapItem.fromDataItem(event.dataItem).dataMap)
        if (currentItem.updateTimestamp > mostRecentTimestamp) {
            newestItem = currentItem
            mostRecentTimestamp = currentItem.updateTimestamp
        }
    }
    return newestItem
}