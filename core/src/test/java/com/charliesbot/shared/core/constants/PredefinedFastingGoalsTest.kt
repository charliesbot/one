package com.charliesbot.shared.core.constants

import androidx.compose.ui.graphics.Color
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PredefinedFastingGoalsTest {

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.e(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        PredefinedFastingGoals.registerCustomGoals(emptyList())
    }

    @Test
    fun `getGoalById returns correct goal for each predefined id`() {
        assertEquals(PredefinedFastingGoals.CIRCADIAN, PredefinedFastingGoals.getGoalById("circadian"))
        assertEquals(PredefinedFastingGoals.SIXTEEN_EIGHT, PredefinedFastingGoals.getGoalById("16:8"))
        assertEquals(PredefinedFastingGoals.EIGHTEEN_SIX, PredefinedFastingGoals.getGoalById("18:6"))
        assertEquals(PredefinedFastingGoals.TWENTY_FOUR, PredefinedFastingGoals.getGoalById("20:4"))
        assertEquals(PredefinedFastingGoals.THIRTY_SIX_HOUR, PredefinedFastingGoals.getGoalById("36hour"))
    }

    @Test
    fun `getGoalById falls back to 16-8 for unknown id`() {
        val result = PredefinedFastingGoals.getGoalById("nonexistent")

        assertEquals("16:8", result.id)
        assertEquals(PredefinedFastingGoals.SIXTEEN_EIGHT, result)
    }

    @Test
    fun `getGoalById returns custom goal after registerCustomGoals`() {
        val customGoal = FastGoal(
            id = "custom_test",
            titleText = "Test Goal",
            durationDisplay = "14",
            color = Color.Red,
            durationMillis = 14 * 60 * 60 * 1000L,
        )

        PredefinedFastingGoals.registerCustomGoals(listOf(customGoal))

        assertEquals(customGoal, PredefinedFastingGoals.getGoalById("custom_test"))
    }

    @Test
    fun `predefined takes priority when custom goal has same id as predefined`() {
        val impostor = FastGoal(
            id = "16:8",
            titleText = "Fake",
            durationDisplay = "99",
            color = Color.Black,
            durationMillis = 99 * 60 * 60 * 1000L,
        )

        PredefinedFastingGoals.registerCustomGoals(listOf(impostor))

        val result = PredefinedFastingGoals.getGoalById("16:8")
        assertEquals(PredefinedFastingGoals.SIXTEEN_EIGHT, result)
    }

    @Test
    fun `allGoals contains exactly 5 goals with correct durations`() {
        val goals = PredefinedFastingGoals.allGoals

        assertEquals(5, goals.size)

        val expectedDurations = mapOf(
            "circadian" to 13L * 60 * 60 * 1000,
            "16:8" to 16L * 60 * 60 * 1000,
            "18:6" to 18L * 60 * 60 * 1000,
            "20:4" to 20L * 60 * 60 * 1000,
            "36hour" to 36L * 60 * 60 * 1000,
        )

        goals.forEach { goal ->
            assertEquals(
                "Duration mismatch for ${goal.id}",
                expectedDurations[goal.id],
                goal.durationMillis,
            )
        }
    }
}
