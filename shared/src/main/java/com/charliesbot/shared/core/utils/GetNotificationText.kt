package com.charliesbot.shared.core.utils

import com.charliesbot.shared.core.models.NotificationContent
import com.charliesbot.shared.core.models.NotificationType
import com.charliesbot.shared.core.abstraction.StringProvider

fun getNotificationText(notificationType: NotificationType, stringProvider: StringProvider): NotificationContent {
    return when (notificationType) {
        NotificationType.ONE_HOUR_BEFORE -> NotificationContent(
            title = stringProvider.getString("notification_one_hour_title"),
            message = stringProvider.getString("notification_one_hour_message")
        )

        NotificationType.COMPLETION -> NotificationContent(
            title = stringProvider.getString("notification_completion_title"),
            message = stringProvider.getString("notification_completion_message")
        )
    }
}