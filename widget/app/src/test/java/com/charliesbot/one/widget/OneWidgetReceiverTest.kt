package com.charliesbot.one.widget

import android.content.Context
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class OneWidgetReceiverTest {
  private lateinit var context: Context
  private lateinit var scheduler: PhoneWidgetRefreshScheduler

  @Before
  fun setup() {
    context = mockk(relaxed = true)
    scheduler = mockk(relaxed = true)

    startKoin { modules(module { single { scheduler } }) }
  }

  @After
  fun teardown() {
    stopKoin()
  }

  @Test
  fun `onEnabled triggers immediate recovery on scheduler`() {
    val receiver = OneWidgetReceiver()

    receiver.onEnabled(context)

    verify(exactly = 1) { scheduler.enqueueImmediateRecovery() }
  }

  @Test
  fun `onDisabled cancels scheduler`() {
    val receiver = OneWidgetReceiver()

    receiver.onDisabled(context)

    verify(exactly = 1) { scheduler.cancel() }
  }
}
