package com.charliesbot.one.features.dashboard.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.charliesbot.shared.core.designsystem.common.R as DesignSystemR
import com.charliesbot.shared.core.strings.R

/** What the active card shows besides the editable details. */
internal data class ActiveHeroState(
  val progress: Float,
  /** Elapsed hours and minutes, e.g. "9:42". */
  val elapsed: String,
  /** Elapsed seconds, e.g. ":15". */
  val elapsedSeconds: String,
  /** "6h 18m to go", then "Goal reached" once the goal passes. */
  val goalStatus: String,
)

/**
 * The active hero's content: the fast dial around the elapsed time, and End fast. [wide] puts the
 * dial on the left with the Started and Goal rows and End fast beside it; on phones those rows sit
 * under the card instead. [motion] ties the dial, timer and End fast into the transition.
 */
@Composable
internal fun ActiveHeroContent(
  state: ActiveHeroState,
  details: FastDetailsState,
  wide: Boolean,
  dialSize: Dp,
  motion: HeroMotion,
  onEndFast: () -> Unit,
  onEditStart: () -> Unit,
  onEditGoal: () -> Unit,
) {
  val endButton: @Composable (Modifier) -> Unit = { buttonModifier ->
    SizedButton(
      text = stringResource(R.string.end_fast),
      icon = DesignSystemR.drawable.stop_24px,
      height = ButtonDefaults.MediumContainerHeight,
      onClick = onEndFast,
      // Squarer than Start fasting's pill: like Play turning into Pause, the shape marks the state.
      shapes =
        ButtonDefaults.shapes(
          shape = RoundedCornerShape(16.dp),
          pressedShape = RoundedCornerShape(8.dp),
        ),
      modifier = buttonModifier.then(motion.action),
    )
  }
  if (wide) {
    Row(
      modifier = Modifier.padding(horizontal = 40.dp, vertical = 20.dp),
      horizontalArrangement = Arrangement.spacedBy(40.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ActiveFastDial(state, dialSize, motion)
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FastDetails(
          modifier = motion.support,
          state = details,
          onEditStart = onEditStart,
          onEditGoal = onEditGoal,
          stacked = true,
        )
        endButton(Modifier.fillMaxWidth())
      }
    }
  } else {
    Column(
      modifier = Modifier.padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      ActiveFastDial(state, dialSize, motion)
      endButton(Modifier.fillMaxWidth())
    }
  }
}

@Composable
private fun ActiveFastDial(state: ActiveHeroState, size: Dp, motion: HeroMotion) {
  // The dial draws in from empty when a fast starts, then follows the clock. Shown on load (or in
  // a preview) it starts settled.
  val startProgress = if (motion.isEntering) 0f else state.progress
  val progress = remember { Animatable(startProgress) }
  val progressSpec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
  LaunchedEffect(state.progress) { progress.animateTo(state.progress, progressSpec) }
  FastDial(progress = { progress.value }, size = size, arcModifier = motion.lead) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Row(
        modifier = motion.support,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          painter = painterResource(DesignSystemR.drawable.local_fire_department_filled_24px),
          contentDescription = null,
          modifier = Modifier.size(20.dp),
        )
        Text(
          stringResource(R.string.fasting_now),
          style = MaterialTheme.typography.labelLargeEmphasized,
        )
      }
      // Long fasts ("100:00:00") or large font scales shrink to fit instead of clipping digits.
      Row(motion.number.scaleToFitWidth()) {
        RollingText(
          state.elapsed,
          style = TodayTextStyles.DialClock,
          modifier = Modifier.alignByBaseline(),
        )
        RollingText(
          state.elapsedSeconds,
          style = MaterialTheme.typography.titleMediumEmphasized,
          modifier = Modifier.alignByBaseline(),
        )
      }
      Text(
        state.goalStatus,
        style = MaterialTheme.typography.titleSmallEmphasized,
        modifier = motion.support,
      )
    }
  }
}
