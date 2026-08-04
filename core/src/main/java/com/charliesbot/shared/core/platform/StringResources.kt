package com.charliesbot.shared.core.platform

import android.content.Context
import com.charliesbot.shared.core.domain.platform.StringKey
import com.charliesbot.shared.core.strings.R

fun Context.getString(key: StringKey): String = getString(key.resourceId)

private val StringKey.resourceId: Int
  get() =
    when (this) {
      StringKey.SETTINGS_EXPORT_SUCCESS -> R.string.settings_export_success
      StringKey.SETTINGS_EXPORT_ERROR -> R.string.settings_export_error
      StringKey.SETTINGS_SYNC_SUCCESS -> R.string.settings_sync_success
      StringKey.SETTINGS_SYNC_ERROR -> R.string.settings_sync_error
      StringKey.SETTINGS_VERSION_COPIED -> R.string.settings_version_copied
      StringKey.NOTIFICATION_ONE_HOUR_TITLE -> R.string.notification_one_hour_title
      StringKey.NOTIFICATION_ONE_HOUR_MESSAGE -> R.string.notification_one_hour_message
      StringKey.NOTIFICATION_COMPLETION_TITLE -> R.string.notification_completion_title
      StringKey.NOTIFICATION_COMPLETION_MESSAGE -> R.string.notification_completion_message
      StringKey.NOTIFICATION_SMART_REMINDER_1H_TITLE ->
        R.string.notification_smart_reminder_1h_title
      StringKey.NOTIFICATION_SMART_REMINDER_1H_MESSAGE ->
        R.string.notification_smart_reminder_1h_message
      StringKey.NOTIFICATION_SMART_REMINDER_START_TITLE ->
        R.string.notification_smart_reminder_start_title
      StringKey.NOTIFICATION_SMART_REMINDER_START_MESSAGE ->
        R.string.notification_smart_reminder_start_message
    }
