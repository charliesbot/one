package com.charliesbot.shared.core.domain.usecase

import com.charliesbot.shared.core.domain.events.FastingEventCallbacks
import com.charliesbot.shared.core.domain.events.FastingEventProcessor
import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateFastingConfigUseCaseTest {

  private val repository: FastingDataRepository = mockk()
  private val eventProcessor: FastingEventProcessor = mockk(relaxed = true)
  private val callbacks: FastingEventCallbacks = mockk(relaxed = true)
  private lateinit var useCase: UpdateFastingConfigUseCase

  @Before
  fun setup() {
    useCase = UpdateFastingConfigUseCase(repository, eventProcessor, callbacks)
  }

  @Test
  fun `returns failure when both params are null`() = runTest {
    val result = useCase(startTimeMillis = null, goalId = null)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalStateException)
  }

  @Test
  fun `returns success when updating config`() = runTest {
    val previous = FastingDataItem(isFasting = true, startTimeInMillis = 1000L)
    val current = FastingDataItem(isFasting = true, startTimeInMillis = 2000L)

    coEvery { repository.updateFastingConfig(2000L, "16:8") } returns Pair(previous, current)

    val result = useCase(startTimeMillis = 2000L, goalId = "16:8")

    assertTrue(result.isSuccess)
    coVerify { repository.updateFastingConfig(2000L, "16:8") }
    coVerify { eventProcessor.processStateChange(previous, current, callbacks) }
  }
}
