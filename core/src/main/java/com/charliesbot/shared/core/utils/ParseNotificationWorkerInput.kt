package com.charliesbot.shared.core.utils

import androidx.work.Data
import com.charliesbot.shared.core.models.NotificationWorkerInput
import com.charliesbot.shared.core.constants.NotificationConstants.NOTIFICATION_TYPE_KEY
import com.charliesbot.shared.core.constants.NotificationConstants.NOTIFICATION_FASTING_START_MILLIS_KEY
import com.charliesbot.shared.core.models.NotificationType

fun parseWorkerInput(inputData: Data): NotificationWorkerInput {
    val notificationTypeEnum = inputData.getString(NOTIFICATION_TYPE_KEY) ?: ""
    val notificationType = NotificationType.valueOf(notificationTypeEnum)
    val fastingStartMillis = inputData.getLong(NOTIFICATION_FASTING_START_MILLIS_KEY, 0L)
    return NotificationWorkerInput(notificationType, fastingStartMillis)
}