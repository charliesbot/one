package com.charliesbot.shared.core.designsystem.app.components

import java.time.LocalDateTime
import org.junit.Assert.*
import org.junit.Test

class DateTimeWheelPickerStateTest {
  @Test
  fun `24 hour wheel includes midnight and preserves evening selection`() {
    val state = DateTimeWheelPickerState(LocalDateTime.of(2026, 9, 9, 18, 30))
    assertEquals((0..23).toList(), state.hourItems(true))
    assertEquals(18, state.hourIndex(true))
    assertEquals(5, state.hourIndex(false))
    state.selectHour(0, true)
    assertEquals(0, state.selectedDateTime.hour)
    assertEquals(30, state.selectedDateTime.minute)
    assertEquals(11, state.hourIndex(false))
    state.selectHour(12, true)
    assertEquals(12, state.selectedDateTime.hour)
    assertEquals(AmPm.PM, state.selectedAmPm)
  }
}
