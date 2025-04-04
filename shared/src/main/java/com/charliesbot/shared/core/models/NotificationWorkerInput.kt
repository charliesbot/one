package com.charliesbot.shared.core.models

data class NotificationWorkerInput(
    val notificationType: NotificationType,
    val fastingStartMillis: Long
)
