package com.charliesbot.shared.core.domain.notifications

import com.charliesbot.shared.core.domain.platform.StringKey
import com.charliesbot.shared.core.domain.platform.StringProvider
import com.charliesbot.shared.core.models.NotificationContent
import com.charliesbot.shared.core.models.NotificationType

fun getNotificationText(
  notificationType: NotificationType,
  stringProvider: StringProvider,
): NotificationContent =
  when (notificationType) {
    NotificationType.ONE_HOUR_BEFORE ->
      NotificationContent(
        title = stringProvider.getString(StringKey.NOTIFICATION_ONE_HOUR_TITLE),
        message = stringProvider.getString(StringKey.NOTIFICATION_ONE_HOUR_MESSAGE),
      )

    NotificationType.COMPLETION ->
      NotificationContent(
        title = stringProvider.getString(StringKey.NOTIFICATION_COMPLETION_TITLE),
        message = stringProvider.getString(StringKey.NOTIFICATION_COMPLETION_MESSAGE),
      )

    NotificationType.SMART_REMINDER_1H_BEFORE ->
      NotificationContent(
        title = stringProvider.getString(StringKey.NOTIFICATION_SMART_REMINDER_1H_TITLE),
        message = stringProvider.getString(StringKey.NOTIFICATION_SMART_REMINDER_1H_MESSAGE),
      )

    NotificationType.SMART_REMINDER_START ->
      NotificationContent(
        title = stringProvider.getString(StringKey.NOTIFICATION_SMART_REMINDER_START_TITLE),
        message = stringProvider.getString(StringKey.NOTIFICATION_SMART_REMINDER_START_MESSAGE),
      )
  }
