package com.charliesbot.one.widget.wear

import android.content.Context
import androidx.glance.wear.core.ActiveWearWidgetHandle
import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.utils.GoalResolver
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class OneWearWidgetTest {
  private lateinit var context: Context
  private lateinit var repository: FastingDataRepository
  private lateinit var goalResolver: GoalResolver
  private lateinit var scheduler: WearWidgetRefreshScheduler

  @Before
  fun setup() {
    context = mockk(relaxed = true)
    repository = mockk(relaxed = true)
    goalResolver = mockk(relaxed = true)
    scheduler = mockk(relaxed = true)

    startKoin {
      modules(
        module {
          single { repository }
          single { goalResolver }
          single { scheduler }
        }
      )
    }
  }

  @After
  fun teardown() {
    stopKoin()
  }

  @Test
  fun `onAdded triggers immediate recovery in scheduler`() = runTest {
    val widget = OneWearWidget()
    val handle = mockk<ActiveWearWidgetHandle>(relaxed = true)

    widget.onAdded(context, handle)

    verify(exactly = 1) { scheduler.enqueueImmediateRecovery() }
  }
}
