package com.charliesbot.shared.core.domain.usecase

import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncFastingStateUseCaseTest {

    private val repository: FastingDataRepository = mockk()
    private lateinit var useCase: SyncFastingStateUseCase

    @Before
    fun setup() {
        useCase = SyncFastingStateUseCase(repository)
    }

    @Test
    fun `calls updateFastingConfig to trigger sync`() = runTest {
        val current = FastingDataItem(isFasting = true, startTimeInMillis = 1000L, fastingGoalId = "16:8")
        coEvery { repository.getCurrentFasting() } returns current
        coEvery { repository.updateFastingConfig(1000L, "16:8") } returns Pair(current, current)

        val result = useCase()

        assertTrue(result.isSuccess)
        coVerify { repository.updateFastingConfig(1000L, "16:8") }
    }
}
