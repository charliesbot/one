package com.charliesbot.shared.core.domain.notifications

import com.charliesbot.shared.core.models.NotificationType

fun generateDismissalId(fastingStartMillis: Long, notificationType: NotificationType): String =
  "${fastingStartMillis}_${notificationType.name}"
