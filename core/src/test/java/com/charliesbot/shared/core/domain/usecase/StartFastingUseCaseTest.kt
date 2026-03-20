package com.charliesbot.shared.core.domain.usecase

import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.services.FastingEventCallbacks
import com.charliesbot.shared.core.services.FastingEventManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StartFastingUseCaseTest {

  private val repository: FastingDataRepository = mockk()
  private val eventManager: FastingEventManager = mockk(relaxed = true)
  private val callbacks: FastingEventCallbacks = mockk(relaxed = true)
  private lateinit var useCase: StartFastingUseCase

  @Before
  fun setup() {
    useCase = StartFastingUseCase(repository, eventManager, callbacks)
  }

  @Test
  fun `returns failure when already fasting`() = runTest {
    coEvery { repository.getCurrentFasting() } returns FastingDataItem(isFasting = true)

    val result = useCase("16:8")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is IllegalStateException)
  }

  @Test
  fun `returns success and calls eventManager on success`() = runTest {
    val previous: FastingDataItem? = null
    val current =
      FastingDataItem(isFasting = true, startTimeInMillis = 1000L, fastingGoalId = "16:8")

    coEvery { repository.getCurrentFasting() } returns null
    coEvery { repository.startFasting(any(), "16:8") } returns Pair(previous, current)

    val result = useCase("16:8")

    assertTrue(result.isSuccess)
    coVerify(ordering = io.mockk.Ordering.ORDERED) {
      repository.startFasting(any(), "16:8")
      eventManager.processStateChange(previous, current, callbacks)
    }
  }

  @Test
  fun `returns failure when repository fails`() = runTest {
    coEvery { repository.getCurrentFasting() } returns null
    coEvery { repository.startFasting(any(), any()) } throws RuntimeException("DB error")

    val result = useCase("16:8")

    assertTrue(result.isFailure)
    assertEquals("DB error", result.exceptionOrNull()?.message)
  }
}
