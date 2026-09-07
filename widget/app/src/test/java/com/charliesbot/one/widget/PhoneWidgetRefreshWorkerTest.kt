package com.charliesbot.one.widget

import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.charliesbot.one.widget.common.WidgetRefreshScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class PhoneWidgetRefreshWorkerTest {
  private lateinit var context: Context
  private lateinit var workerParams: WorkerParameters
  private lateinit var scheduler: WidgetRefreshScheduler

  @Before
  fun setup() {
    context = mockk(relaxed = true)
    workerParams = mockk(relaxed = true) { every { runAttemptCount } returns 0 }
    scheduler = mockk(relaxed = true)
    startKoin { modules(module { single { scheduler } }) }
  }

  @After
  fun teardown() {
    stopKoin()
  }

  @Test
  fun `exposes the constructor used by WorkManager`() {
    assertNotNull(
      PhoneWidgetRefreshWorker::class
        .java
        .getConstructor(Context::class.java, WorkerParameters::class.java)
    )
  }

  @Test
  fun `delegates refresh to shared scheduler and returns success`() = runTest {
    val worker = PhoneWidgetRefreshWorker(context, workerParams)

    assertEquals(Result.success(), worker.doWork())
    coVerify(exactly = 1) { scheduler.refresh() }
  }

  @Test
  fun `retries refresh failures before retry limit`() = runTest {
    coEvery { scheduler.refresh() } throws IllegalStateException("Update failed")

    for (attempt in 0..2) {
      every { workerParams.runAttemptCount } returns attempt
      val worker = PhoneWidgetRefreshWorker(context, workerParams)
      assertEquals(Result.retry(), worker.doWork())
    }
  }

  @Test
  fun `fails refresh after retry limit`() = runTest {
    every { workerParams.runAttemptCount } returns 3
    coEvery { scheduler.refresh() } throws IllegalStateException("Update failed")
    val worker = PhoneWidgetRefreshWorker(context, workerParams)

    assertEquals(Result.failure(), worker.doWork())
  }

  @Test(expected = CancellationException::class)
  fun `propagates cancellation without retrying`() = runTest {
    coEvery { scheduler.refresh() } throws CancellationException("Worker cancelled")

    PhoneWidgetRefreshWorker(context, workerParams).doWork()
  }
}
