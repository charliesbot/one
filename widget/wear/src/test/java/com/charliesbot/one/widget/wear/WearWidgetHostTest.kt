package com.charliesbot.one.widget.wear

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WearWidgetHostTest {
  private lateinit var context: Context

  @Before
  fun setup() {
    context = mockk(relaxed = true)
  }

  @Test
  fun `hasActiveWidgets delegates to activeWidgetChecker and canEnqueueImmediateRecovery is true`() =
    runTest {
      var active = true
      val adapter = WearWidgetHost(context = context, activeWidgetChecker = { active })

      assertTrue(adapter.hasActiveWidgets())
      assertTrue(adapter.canEnqueueImmediateRecovery())

      active = false
      assertFalse(adapter.hasActiveWidgets())
      assertTrue(adapter.canEnqueueImmediateRecovery()) // Always true on Wear
    }

  @Test
  fun `requestWidgetUpdate invokes widget updater`() = runTest {
    var updateCalled = false
    val adapter =
      WearWidgetHost(
        context = context,
        activeWidgetChecker = { true },
        widgetUpdater = { updateCalled = true },
      )

    adapter.requestWidgetUpdate()

    assertTrue(updateCalled)
  }
}
