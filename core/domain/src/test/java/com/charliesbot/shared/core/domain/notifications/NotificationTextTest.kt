package com.charliesbot.shared.core.domain.notifications

import com.charliesbot.shared.core.domain.platform.StringProvider
import com.charliesbot.shared.core.models.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationTextTest {

  private val stringProvider =
    object : StringProvider {
      override fun getString(resourceId: String): String = resourceId
    }

  @Test
  fun `one hour before notification uses expected string keys`() {
    val text = getNotificationText(NotificationType.ONE_HOUR_BEFORE, stringProvider)

    assertEquals("notification_one_hour_title", text.title)
    assertEquals("notification_one_hour_message", text.message)
  }

  @Test
  fun `completion notification uses expected string keys`() {
    val text = getNotificationText(NotificationType.COMPLETION, stringProvider)

    assertEquals("notification_completion_title", text.title)
    assertEquals("notification_completion_message", text.message)
  }

  @Test
  fun `smart reminder one hour notification uses expected string keys`() {
    val text = getNotificationText(NotificationType.SMART_REMINDER_1H_BEFORE, stringProvider)

    assertEquals("notification_smart_reminder_1h_title", text.title)
    assertEquals("notification_smart_reminder_1h_message", text.message)
  }

  @Test
  fun `smart reminder start notification uses expected string keys`() {
    val text = getNotificationText(NotificationType.SMART_REMINDER_START, stringProvider)

    assertEquals("notification_smart_reminder_start_title", text.title)
    assertEquals("notification_smart_reminder_start_message", text.message)
  }
}
