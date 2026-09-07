package com.charliesbot.one.widget.wear

import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.charliesbot.one.widget.common.WidgetRefreshScheduler
import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.utils.GoalResolver
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class WearWidgetRefreshWorkerTest {
  private lateinit var context: Context
  private lateinit var workerParams: WorkerParameters
  private lateinit var repository: FastingDataRepository
  private lateinit var goalResolver: GoalResolver
  private lateinit var scheduler: WidgetRefreshScheduler
  private var widgetUpdateCount = 0
  private val testWidgetUpdater: suspend (Context) -> Unit = { widgetUpdateCount++ }

  private val oneHourMillis = 60L * 60L * 1000L

  @Before
  fun setup() {
    context = mockk(relaxed = true)
    workerParams = mockk(relaxed = true) { every { runAttemptCount } returns 0 }
    repository = mockk()
    goalResolver = mockk()
    scheduler = mockk(relaxed = true)
    widgetUpdateCount = 0

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
  fun `cancels scheduler and returns success when not fasting`() = runTest {
    coEvery { repository.getCurrentFasting() } returns FastingDataItem(isFasting = false)

    val worker = WearWidgetRefreshWorker(context, workerParams, testWidgetUpdater)
    val result = worker.doWork()

    assertEquals(Result.success(), result)
    assertEquals(1, widgetUpdateCount)
    coVerify { scheduler.onFastingCompleted() }
    coVerify(exactly = 0) { scheduler.onWorkerTickCompleted(any(), any(), any()) }
  }

  @Test
  fun `updates widgets and delegates to onWorkerTickCompleted when fast is active`() = runTest {
    val startTime = System.currentTimeMillis() - (5 * oneHourMillis)
    val goalDuration = 16 * oneHourMillis

    coEvery { repository.getCurrentFasting() } returns
      FastingDataItem(isFasting = true, startTimeInMillis = startTime, fastingGoalId = "16:8")
    coEvery { goalResolver.durationMillis("16:8") } returns goalDuration

    val worker = WearWidgetRefreshWorker(context, workerParams, testWidgetUpdater)
    val result = worker.doWork()

    assertEquals(Result.success(), result)
    assertEquals(1, widgetUpdateCount)
    coVerify {
      scheduler.onWorkerTickCompleted(
        snapshotStartTime = startTime,
        snapshotGoalId = "16:8",
        goalDurationMillis = goalDuration,
        currentTimeMillis = any(),
      )
    }
  }

  @Test
  fun `retries on recoverable exception when runAttemptCount is less than 3`() = runTest {
    coEvery { repository.getCurrentFasting() } throws RuntimeException("Glance Wear render failure")

    val worker = WearWidgetRefreshWorker(context, workerParams, testWidgetUpdater)
    val result = worker.doWork()

    assertEquals(Result.retry(), result)
  }

  @Test(expected = CancellationException::class)
  fun `rethrows CancellationException without swallowing`() = runTest {
    coEvery { repository.getCurrentFasting() } throws CancellationException("Worker cancelled")

    val worker = WearWidgetRefreshWorker(context, workerParams, testWidgetUpdater)
    worker.doWork()
  }
}
