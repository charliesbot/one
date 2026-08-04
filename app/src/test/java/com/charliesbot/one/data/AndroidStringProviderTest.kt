package com.charliesbot.one.data

import android.content.Context
import com.charliesbot.shared.core.domain.platform.StringKey
import com.charliesbot.shared.core.strings.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidStringProviderTest {

  private val context: Context = mockk()
  private val provider = AndroidStringProvider(context)

  @Test
  fun `every string key resolves through a generated resource id`() {
    val resourceIds =
      mapOf(
        StringKey.SETTINGS_EXPORT_SUCCESS to R.string.settings_export_success,
        StringKey.SETTINGS_EXPORT_ERROR to R.string.settings_export_error,
        StringKey.SETTINGS_SYNC_SUCCESS to R.string.settings_sync_success,
        StringKey.SETTINGS_SYNC_ERROR to R.string.settings_sync_error,
        StringKey.SETTINGS_VERSION_COPIED to R.string.settings_version_copied,
        StringKey.NOTIFICATION_ONE_HOUR_TITLE to R.string.notification_one_hour_title,
        StringKey.NOTIFICATION_ONE_HOUR_MESSAGE to R.string.notification_one_hour_message,
        StringKey.NOTIFICATION_COMPLETION_TITLE to R.string.notification_completion_title,
        StringKey.NOTIFICATION_COMPLETION_MESSAGE to R.string.notification_completion_message,
        StringKey.NOTIFICATION_SMART_REMINDER_1H_TITLE to
          R.string.notification_smart_reminder_1h_title,
        StringKey.NOTIFICATION_SMART_REMINDER_1H_MESSAGE to
          R.string.notification_smart_reminder_1h_message,
        StringKey.NOTIFICATION_SMART_REMINDER_START_TITLE to
          R.string.notification_smart_reminder_start_title,
        StringKey.NOTIFICATION_SMART_REMINDER_START_MESSAGE to
          R.string.notification_smart_reminder_start_message,
      )

    resourceIds.forEach { (key, resourceId) ->
      every { context.getString(resourceId) } returns key.name

      assertEquals(key.name, provider.getString(key))
    }
  }
}
