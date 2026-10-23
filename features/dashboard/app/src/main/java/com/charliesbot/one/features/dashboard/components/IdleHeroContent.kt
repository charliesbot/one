package com.charliesbot.one.features.dashboard.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.charliesbot.one.features.dashboard.model.ClockText
import com.charliesbot.shared.core.designsystem.common.R as DesignSystemR
import com.charliesbot.shared.core.strings.R

/**
 * What the idle card shows. With a suggested start it answers "when should I start?"; without one,
 * "if I start now, when do I finish?". Both use the same layout.
 */
internal data class IdleHeroState(
  val eyebrow: String,
  @param:DrawableRes val eyebrowIcon: Int,
  val clock: ClockText,
  val detail: String,
  val windowStart: WindowEdge,
  val windowEnd: WindowEdge,
  val bedtimeFraction: Float?,
  val bedtimeLabel: String?,
  val windowDescription: String,
  val goalLabel: String,
)

/**
 * The idle hero's content: the big time, why it was picked, the fast window, and Start fasting.
 * [wide] lays it out sideways for large screens, with the window as a full-width footer. [motion]
 * ties the clock, Start fasting and the supporting text into the start/end transition.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun IdleHeroContent(
  state: IdleHeroState,
  wide: Boolean,
  motion: HeroMotion,
  onStartFasting: () -> Unit,
  onChangeGoal: () -> Unit,
) {
  val window: @Composable (Modifier) -> Unit = { windowModifier ->
    FastWindowTimeline(
      start = state.windowStart,
      end = state.windowEnd,
      bedtimeFraction = state.bedtimeFraction,
      bedtimeLabel = state.bedtimeLabel,
      description = state.windowDescription,
      modifier = windowModifier,
    )
  }
  val startButton: @Composable (Modifier) -> Unit = { buttonModifier ->
    SizedButton(
      text = stringResource(R.string.start_fasting),
      icon = DesignSystemR.drawable.play_arrow_24px,
      height = ButtonDefaults.LargeContainerHeight,
      onClick = onStartFasting,
      modifier = buttonModifier.then(motion.action),
    )
  }

  if (wide) {
    Column(Modifier.padding(horizontal = 40.dp, vertical = 24.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Eyebrow(state, motion.support)
          Clock(state.clock, motion.number)
          Text(state.detail, style = MaterialTheme.typography.bodyLarge, modifier = motion.support)
        }
        Spacer(Modifier.size(48.dp))
        startButton(Modifier.weight(1f).widthIn(max = 320.dp))
      }
      Spacer(Modifier.height(16.dp))
      window(motion.support)
    }
  } else {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Eyebrow(state, Modifier.weight(1f).then(motion.support))
        SizedElevatedButton(
          text = state.goalLabel,
          icon = DesignSystemR.drawable.timer_24px,
          height = ButtonDefaults.ExtraSmallContainerHeight,
          onClick = onChangeGoal,
        )
      }
      Clock(state.clock, Modifier.padding(top = 8.dp).then(motion.number))
      Text(state.detail, style = MaterialTheme.typography.bodyLarge, modifier = motion.support)
      window(Modifier.padding(top = 16.dp).then(motion.support))
      Spacer(Modifier.height(16.dp))
      startButton(Modifier.fillMaxWidth())
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Eyebrow(state: IdleHeroState, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      painter = painterResource(state.eyebrowIcon),
      contentDescription = null,
      modifier = Modifier.size(20.dp),
    )
    Text(state.eyebrow, style = MaterialTheme.typography.labelLargeEmphasized)
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Clock(clock: ClockText, modifier: Modifier = Modifier) {
  Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(clock.time, style = TodayTextStyles.HeroClock, modifier = Modifier.alignByBaseline())
    clock.period?.let {
      Text(
        it,
        style = MaterialTheme.typography.headlineLargeEmphasized,
        modifier = Modifier.alignByBaseline(),
      )
    }
  }
}
