package com.charliesbot.shared.core.utils

import androidx.compose.ui.graphics.Color
import com.charliesbot.shared.core.constants.FastGoal
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.domain.repository.CustomGoalRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
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

  @After
  fun tearDown() {
    PredefinedFastingGoals.registerCustomGoals(emptyList())
  }

  @Test
  fun `emits only 5 predefined goals when no custom goals exist`() = runTest {
    every { customGoalRepository.customGoals } returns flowOf(emptyList())

    val resolver = GoalResolver(customGoalRepository)
    val goals = resolver.allGoals.first()

    assertEquals(5, goals.size)
    assertEquals(PredefinedFastingGoals.allGoals, goals)
  }

  @Test
  fun `emits predefined plus custom goals combined`() = runTest {
    val customGoal =
      FastGoal(
        id = "custom_1",
        titleText = "My Goal",
        durationDisplay = "14",
        color = Color.Red,
        durationMillis = 14 * 60 * 60 * 1000L,
      )
    every { customGoalRepository.customGoals } returns flowOf(listOf(customGoal))

    val resolver = GoalResolver(customGoalRepository)
    val goals = resolver.allGoals.first()

    assertEquals(6, goals.size)
    assertTrue(goals.containsAll(PredefinedFastingGoals.allGoals))
    assertTrue(goals.contains(customGoal))
  }

  @Test
  fun `collecting the flow registers custom goals so getGoalById resolves them`() = runTest {
    val customGoal =
      FastGoal(
        id = "custom_2",
        titleText = "Custom Goal",
        durationDisplay = "15",
        color = Color.Blue,
        durationMillis = 15 * 60 * 60 * 1000L,
      )
    every { customGoalRepository.customGoals } returns flowOf(listOf(customGoal))

    val resolver = GoalResolver(customGoalRepository)
    resolver.allGoals.first() // trigger collection which calls registerCustomGoals

    val resolved = PredefinedFastingGoals.getGoalById("custom_2")
    assertEquals(customGoal, resolved)
  }

  @Test
  fun `collecting with empty list clears previously registered custom goals`() = runTest {
    // First register a custom goal
    val customGoal =
      FastGoal(
        id = "custom_3",
        titleText = "Temp Goal",
        durationDisplay = "22",
        color = Color.Green,
        durationMillis = 22 * 60 * 60 * 1000L,
      )
    PredefinedFastingGoals.registerCustomGoals(listOf(customGoal))
    assertEquals(customGoal, PredefinedFastingGoals.getGoalById("custom_3"))

    // Now collect with empty list
    every { customGoalRepository.customGoals } returns flowOf(emptyList())
    val resolver = GoalResolver(customGoalRepository)
    resolver.allGoals.first()

    // custom_3 should no longer resolve — falls back to 16:8
    val resolved = PredefinedFastingGoals.getGoalById("custom_3")
    assertEquals("16:8", resolved.id)
  }
}
