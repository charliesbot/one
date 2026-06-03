package com.charliesbot.shared.core.domain.usecase

import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.services.FastingEventCallbacks
import com.charliesbot.shared.core.services.FastingEventProcessor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StopFastingUseCaseTest {

  private val repository: FastingDataRepository = mockk()
  private val eventProcessor: FastingEventProcessor = mockk(relaxed = true)
  private val callbacks: FastingEventCallbacks = mockk(relaxed = true)
  private lateinit var useCase: StopFastingUseCase

  @Before
  fun setup() {
    useCase = StopFastingUseCase(repository, eventProcessor, callbacks)
  }

  @Test
  fun `returns success when not fasting`() = runTest {
    coEvery { repository.getCurrentFasting() } returns FastingDataItem(isFasting = false)

    val result = useCase()

    assertTrue(result.isSuccess)
    coVerify(exactly = 0) { repository.stopFasting(any()) }
  }

  @Test
  fun `returns success and calls eventProcessor when fasting`() = runTest {
    val existing = FastingDataItem(isFasting = true, fastingGoalId = "18:6")
    val previous = FastingDataItem(isFasting = true)
    val current = FastingDataItem(isFasting = false)

    coEvery { repository.getCurrentFasting() } returns existing
    coEvery { repository.stopFasting("18:6") } returns Pair(previous, current)

    val result = useCase()

    assertTrue(result.isSuccess)
    coVerify(ordering = io.mockk.Ordering.ORDERED) {
      repository.stopFasting("18:6")
      eventProcessor.processStateChange(previous, current, callbacks)
    }
  }
}
