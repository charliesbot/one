package com.charliesbot.one.widget

import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.utils.GoalResolver
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class PhoneWidgetRefreshWorkerTest {
  private lateinit var context: Context
  private lateinit var workerParams: WorkerParameters
  private lateinit var repository: FastingDataRepository
  private lateinit var goalResolver: GoalResolver
  private lateinit var scheduler: PhoneWidgetRefreshScheduler
  private var widgetUpdateCount = 0
  private val testWidgetUpdater: suspend (Context) -> Unit = { widgetUpdateCount++ }

  private val oneHourMillis = 60L * 60L * 1000L

  @Before
  fun setup() {
    context = mockk(relaxed = true)
    workerParams = mockk(relaxed = true)
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

    val worker = PhoneWidgetRefreshWorker(context, workerParams, testWidgetUpdater)
    val result = worker.doWork()

    assertEquals(Result.success(), result)
    assertEquals(1, widgetUpdateCount)
    verify { scheduler.cancel() }
    verify(exactly = 0) { scheduler.scheduleNext(any(), any(), any()) }
  }

  @Test
  fun `schedules next boundary and returns success when fast is active`() = runTest {
    val startTime = System.currentTimeMillis() - (5 * oneHourMillis)
    val goalDuration = 16 * oneHourMillis

    coEvery { repository.getCurrentFasting() } returns FastingDataItem(
      isFasting = true,
      startTimeInMillis = startTime,
      fastingGoalId = "16:8",
    )
    coEvery { goalResolver.resolveGoalDurationMillis("16:8") } returns goalDuration

    val worker = PhoneWidgetRefreshWorker(context, workerParams, testWidgetUpdater)
    val result = worker.doWork()

    assertEquals(Result.success(), result)
    assertEquals(1, widgetUpdateCount)
    verify {
      scheduler.scheduleNext(
        startTimeMillis = startTime,
        goalDurationMillis = goalDuration,
        currentTimeMillis = any(),
      )
    }
  }

  @Test
  fun `cancels scheduler and returns success when goal already exceeded`() = runTest {
    val startTime = System.currentTimeMillis() - (17 * oneHourMillis)
    val goalDuration = 16 * oneHourMillis

    coEvery { repository.getCurrentFasting() } returns FastingDataItem(
      isFasting = true,
      startTimeInMillis = startTime,
      fastingGoalId = "16:8",
    )
    coEvery { goalResolver.resolveGoalDurationMillis("16:8") } returns goalDuration

    val worker = PhoneWidgetRefreshWorker(context, workerParams, testWidgetUpdater)
    val result = worker.doWork()

    assertEquals(Result.success(), result)
    assertEquals(1, widgetUpdateCount)
    verify { scheduler.cancel() }
    verify(exactly = 0) { scheduler.scheduleNext(any(), any(), any()) }
  }
}
