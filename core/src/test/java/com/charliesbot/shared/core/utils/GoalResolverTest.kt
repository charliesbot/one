package com.charliesbot.shared.core.utils

import com.charliesbot.shared.core.domain.repository.CustomGoalRepository
import com.charliesbot.shared.core.models.CustomGoalData
import com.charliesbot.shared.core.models.FastingGoalCatalog
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GoalResolverTest {

  private lateinit var customGoalRepository: CustomGoalRepository

  @Before
  fun setup() {
    customGoalRepository = mockk()
  }

  @Test
  fun `emits only 5 predefined goals when no custom goals exist`() = runTest {
    every { customGoalRepository.customGoals } returns flowOf(emptyList())

    val resolver = GoalResolver(customGoalRepository)
    val goals = resolver.allGoals.first()

    assertEquals(5, goals.size)
    assertEquals(FastingGoalCatalog.allGoals, goals)
  }

  @Test
  fun `emits predefined plus custom goals combined`() = runTest {
    val customGoal =
      CustomGoalData(
        id = "custom_1",
        name = "My Goal",
        durationMillis = 14 * 60 * 60 * 1000L,
        colorHex = 0xFFFF0000,
      )
    every { customGoalRepository.customGoals } returns flowOf(listOf(customGoal))

    val resolver = GoalResolver(customGoalRepository)
    val goals = resolver.allGoals.first()

    assertEquals(6, goals.size)
    assertTrue(goals.containsAll(FastingGoalCatalog.allGoals))
    assertEquals(customGoal.toFastingGoal(), goals.last())
  }

  @Test
  fun `custom goal keeps display metadata as pure scalar values`() = runTest {
    val customGoal =
      CustomGoalData(
        id = "custom_2",
        name = "Custom Goal",
        durationMillis = 15 * 60 * 60 * 1000L,
        colorHex = 0xFF0000FF,
      )
    every { customGoalRepository.customGoals } returns flowOf(listOf(customGoal))
    val resolver = GoalResolver(customGoalRepository)

    val resolved = resolver.allGoals.first().last()

    assertEquals("custom_2", resolved.id)
    assertEquals("Custom Goal", resolved.name)
    assertEquals("15", resolved.durationDisplay)
    assertEquals(0xFF0000FF, resolved.colorHex)
  }
}
