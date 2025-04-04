package com.charliesbot.shared.core.utils

import com.charliesbot.shared.core.models.NotificationContent
import com.charliesbot.shared.core.models.NotificationType

fun getNotificationText(notificationType: NotificationType): NotificationContent {
    return when (notificationType) {
        NotificationType.ONE_HOUR_BEFORE -> NotificationContent(
            title = "1 Hour Remaining!",
            message = "You're doing great! Just one more hour until your fast is complete."
        )

        NotificationType.COMPLETION -> NotificationContent(
            title = "Fasting Completed!",
            message = "Congratulations! You have completed your fast."
        )
    }
}