package com.charliesbot.one.widget

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PhoneWidgetHostTest {
  private lateinit var context: Context

  @Before
  fun setup() {
    context = mockk(relaxed = true)
  }

  @Test
  fun `hasActiveWidgets and canEnqueueImmediateRecovery delegate to activeWidgetChecker`() =
    runTest {
      var active = true
      val adapter = PhoneWidgetHost(context = context, activeWidgetChecker = { active })

      assertTrue(adapter.hasActiveWidgets())
      assertTrue(adapter.canEnqueueImmediateRecovery())

      active = false
      assertFalse(adapter.hasActiveWidgets())
      assertFalse(adapter.canEnqueueImmediateRecovery())
    }

  @Test
  fun `requestWidgetUpdate invokes widget updater`() = runTest {
    var updateCalled = false
    val adapter =
      PhoneWidgetHost(
        context = context,
        activeWidgetChecker = { true },
        widgetUpdater = { updateCalled = true },
      )

    adapter.requestWidgetUpdate()

    assertTrue(updateCalled)
  }
}
