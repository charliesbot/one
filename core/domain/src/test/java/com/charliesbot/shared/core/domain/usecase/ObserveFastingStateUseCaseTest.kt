package com.charliesbot.shared.core.domain.usecase

import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ObserveFastingStateUseCaseTest {

  private val repository: FastingDataRepository = mockk()
  private lateinit var useCase: ObserveFastingStateUseCase

  @Before
  fun setup() {
    useCase = ObserveFastingStateUseCase(repository)
  }

  @Test
  fun `returns fasting flow from repository`() = runTest {
    val item = FastingDataItem(isFasting = true)
    every { repository.fastingDataItem } returns flowOf(item)

    val result = useCase().first()

    assertEquals(item, result)
  }
}
