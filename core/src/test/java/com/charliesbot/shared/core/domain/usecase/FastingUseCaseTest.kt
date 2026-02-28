package com.charliesbot.shared.core.domain.usecase

import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.services.FastingEventCallbacks
import com.charliesbot.shared.core.services.FastingEventManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class FastingUseCaseTest {

    private lateinit var repository: FastingDataRepository
    private lateinit var eventManager: FastingEventManager
    private lateinit var callbacks: FastingEventCallbacks
    private lateinit var useCase: FastingUseCase

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0

        repository = mockk()
        eventManager = mockk(relaxed = true)
        callbacks = mockk(relaxed = true)
        useCase = FastingUseCase(repository, eventManager, callbacks)
    }

    // ============= startFasting Tests =============

    @Test
    fun `startFasting throws IllegalStateException when already fasting`() = runTest {
        coEvery { repository.getCurrentFasting() } returns FastingDataItem(isFasting = true)

        assertThrows(IllegalStateException::class.java) {
            runTest { useCase.startFasting("16:8") }
        }
    }

    @Test
    fun `startFasting calls repository then eventManager on success`() = runTest {
        val previous: FastingDataItem? = null
        val current = FastingDataItem(isFasting = true, startTimeInMillis = 1000L, fastingGoalId = "16:8")

        coEvery { repository.getCurrentFasting() } returns null
        coEvery { repository.startFasting(any(), eq("16:8")) } returns Pair(previous, current)

        useCase.startFasting("16:8")

        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            repository.startFasting(any(), "16:8")
            eventManager.processStateChange(previous, current, callbacks)
        }
    }

    @Test
    fun `startFasting rethrows repository exceptions`() = runTest {
        coEvery { repository.getCurrentFasting() } returns null
        coEvery { repository.startFasting(any(), any()) } throws RuntimeException("DB error")

        assertThrows(RuntimeException::class.java) {
            runTest { useCase.startFasting("16:8") }
        }
    }

    // ============= stopFasting Tests =============

    @Test
    fun `stopFasting returns early when isFasting is false`() = runTest {
        coEvery { repository.getCurrentFasting() } returns FastingDataItem(isFasting = false)

        useCase.stopFasting()

        coVerify(exactly = 0) { repository.stopFasting(any()) }
        coVerify(exactly = 0) { eventManager.processStateChange(any(), any(), any()) }
    }

    @Test
    fun `stopFasting returns early when getCurrentFasting returns null`() = runTest {
        coEvery { repository.getCurrentFasting() } returns null

        useCase.stopFasting()

        coVerify(exactly = 0) { repository.stopFasting(any()) }
        coVerify(exactly = 0) { eventManager.processStateChange(any(), any(), any()) }
    }

    @Test
    fun `stopFasting calls repository with correct goalId then eventManager`() = runTest {
        val existing = FastingDataItem(isFasting = true, fastingGoalId = "18:6")
        val previous = FastingDataItem(isFasting = true)
        val current = FastingDataItem(isFasting = false)

        coEvery { repository.getCurrentFasting() } returns existing
        coEvery { repository.stopFasting("18:6") } returns Pair(previous, current)

        useCase.stopFasting()

        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            repository.stopFasting("18:6")
            eventManager.processStateChange(previous, current, callbacks)
        }
    }

    // ============= updateFastingConfig Tests =============

    @Test
    fun `updateFastingConfig throws when both params are null`() = runTest {
        assertThrows(IllegalStateException::class.java) {
            runTest { useCase.updateFastingConfig(startTimeMillis = null, goalId = null) }
        }
    }

    @Test
    fun `updateFastingConfig succeeds with only startTimeMillis`() = runTest {
        val previous = FastingDataItem(isFasting = true, startTimeInMillis = 1000L)
        val current = FastingDataItem(isFasting = true, startTimeInMillis = 2000L)

        coEvery { repository.updateFastingConfig(2000L, null) } returns Pair(previous, current)

        useCase.updateFastingConfig(startTimeMillis = 2000L)

        coVerify { repository.updateFastingConfig(2000L, null) }
        coVerify { eventManager.processStateChange(previous, current, callbacks) }
    }

    @Test
    fun `updateFastingConfig succeeds with only goalId`() = runTest {
        val previous = FastingDataItem(isFasting = true, fastingGoalId = "16:8")
        val current = FastingDataItem(isFasting = true, fastingGoalId = "20:4")

        coEvery { repository.updateFastingConfig(null, "20:4") } returns Pair(previous, current)

        useCase.updateFastingConfig(goalId = "20:4")

        coVerify { repository.updateFastingConfig(null, "20:4") }
        coVerify { eventManager.processStateChange(previous, current, callbacks) }
    }
}
