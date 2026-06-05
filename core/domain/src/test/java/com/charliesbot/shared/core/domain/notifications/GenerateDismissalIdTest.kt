package com.charliesbot.shared.core.domain.notifications

import com.charliesbot.shared.core.models.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Test

class GenerateDismissalIdTest {

  @Test
  fun `dismissal id combines fasting start millis and notification type`() {
    val dismissalId =
      generateDismissalId(
        fastingStartMillis = 123456789L,
        notificationType = NotificationType.COMPLETION,
      )

    assertEquals("123456789_COMPLETION", dismissalId)
  }
}
