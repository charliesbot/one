package com.charliesbot.one.features.dashboard.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * Text whose characters roll up one by one when they change, like an odometer, so a ticking timer
 * reads as moving instead of flickering. Meant for tabular numerals, where every digit is the same
 * width. Accessibility reads the whole string, not the individual characters.
 */
@Composable
internal fun RollingText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
  // Each character is its own Text, so the style's letter spacing goes between them instead.
  val letterSpacing =
    with(LocalDensity.current) {
      if (style.letterSpacing.isSp) style.letterSpacing.toDp() else 0.dp
    }
  val charStyle = style.copy(letterSpacing = TextUnit.Unspecified)
  Row(
    modifier = modifier.clearAndSetSemantics { contentDescription = text },
    horizontalArrangement = Arrangement.spacedBy(letterSpacing),
  ) {
    text.forEach { char -> RollingChar(char, charStyle) }
  }
}

/**
 * One character that rolls to its new value. The roll is a draw-time offset inside a clipped box:
 * layout and baseline never move, so text aligned to this baseline stays still while it rolls.
 */
@Composable
private fun RollingChar(char: Char, style: TextStyle) {
  val motion = MaterialTheme.motionScheme
  var shown by remember { mutableStateOf(char) }
  var previous by remember { mutableStateOf<Char?>(null) }
  // 0 = the previous character in place, 1 = the new one in place.
  val roll = remember { Animatable(1f) }
  LaunchedEffect(char) {
    if (char != shown) {
      previous = shown
      shown = char
      roll.snapTo(0f)
      roll.animateTo(1f, motion.fastSpatialSpec())
      previous = null
    }
  }
  Box(Modifier.clipToBounds()) {
    previous?.let { old ->
      Text(
        old.toString(),
        style = style,
        modifier =
          Modifier.graphicsLayer {
            translationY = -size.height * roll.value
            alpha = (1f - roll.value).coerceIn(0f, 1f)
          },
      )
    }
    Text(
      shown.toString(),
      style = style,
      modifier =
        Modifier.graphicsLayer {
          translationY = size.height * (1f - roll.value)
          alpha = roll.value.coerceIn(0f, 1f)
        },
    )
  }
}
