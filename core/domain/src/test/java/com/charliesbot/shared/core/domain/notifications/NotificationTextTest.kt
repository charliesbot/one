package com.charliesbot.shared.core.domain.notifications

import com.charliesbot.shared.core.domain.platform.StringKey
import com.charliesbot.shared.core.domain.platform.StringProvider
import com.charliesbot.shared.core.models.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationTextTest {

  private val stringProvider =
    object : StringProvider {
      override fun getString(key: StringKey): String = key.name
    }

  @Test
  fun `one hour before notification uses expected string keys`() {
    val text = getNotificationText(NotificationType.ONE_HOUR_BEFORE, stringProvider)

    assertEquals("NOTIFICATION_ONE_HOUR_TITLE", text.title)
    assertEquals("NOTIFICATION_ONE_HOUR_MESSAGE", text.message)
  }

  @Test
  fun `completion notification uses expected string keys`() {
    val text = getNotificationText(NotificationType.COMPLETION, stringProvider)

    assertEquals("NOTIFICATION_COMPLETION_TITLE", text.title)
    assertEquals("NOTIFICATION_COMPLETION_MESSAGE", text.message)
  }

  @Test
  fun `smart reminder one hour notification uses expected string keys`() {
    val text = getNotificationText(NotificationType.SMART_REMINDER_1H_BEFORE, stringProvider)

    assertEquals("NOTIFICATION_SMART_REMINDER_1H_TITLE", text.title)
    assertEquals("NOTIFICATION_SMART_REMINDER_1H_MESSAGE", text.message)
  }

  @Test
  fun `smart reminder start notification uses expected string keys`() {
    val text = getNotificationText(NotificationType.SMART_REMINDER_START, stringProvider)

    assertEquals("NOTIFICATION_SMART_REMINDER_START_TITLE", text.title)
    assertEquals("NOTIFICATION_SMART_REMINDER_START_MESSAGE", text.message)
  }
}
