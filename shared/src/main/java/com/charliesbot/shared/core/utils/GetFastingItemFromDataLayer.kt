package com.charliesbot.shared.core.utils

import com.charliesbot.shared.core.constants.DataLayerConstants
import com.charliesbot.shared.core.models.FastingDataItem
import com.google.android.gms.wearable.DataMap

fun getFastingItemFromDataLayer(dataMap: DataMap): FastingDataItem {
    val isFasting = dataMap.getBoolean(DataLayerConstants.IS_FASTING_KEY, false)
    val startTime = dataMap.getLong(DataLayerConstants.START_TIME_KEY, 0L)
    val updateTimestamp = dataMap.getLong(DataLayerConstants.UPDATE_TIMESTAMP_KEY, 0L)
    val fastingGoalId = dataMap.getString(DataLayerConstants.FASTING_GOAL_KEY) ?: ""
    return FastingDataItem(isFasting, startTime, updateTimestamp, fastingGoalId)
}