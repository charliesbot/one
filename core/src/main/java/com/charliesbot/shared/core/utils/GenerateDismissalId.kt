package com.charliesbot.shared.core.utils

import com.charliesbot.shared.core.models.NotificationType

fun generateDismissalId(fastingStartMillis: Long, notificationType: NotificationType): String {
    return "${fastingStartMillis}_${notificationType.name}"
}