package com.charliesbot.one.features.dashboard.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

private const val PrimaryActionKey = "today-primary-action"
private const val HeroNumberKey = "today-hero-number"

/** What the card shows while fasting: the dial's state and the editable details. */
internal data class ActiveHero(val dial: ActiveHeroState, val details: FastDetailsState)

/**
 * Modifiers that tie one hero content's pieces into the start/end transition. Contents apply them
 * to the matching pieces and stay unaware of how the transition is built.
 */
internal class HeroMotion(
  /** Start fasting and End fast: one button that morphs in place. */
  val action: Modifier,
  /** The big number: the next fast's time travels into the dial as the timer, and back. */
  val number: Modifier,
  /** The piece that lands first and blooms in: the dial. */
  val lead: Modifier,
  /** Supporting pieces that settle in just after the lead. */
  val support: Modifier,
  /** True when this content arrives through a start or end, false when it is shown on load. */
  val isEntering: Boolean,
)

/**
 * The Today hero: one card that stays on screen when a fast starts or ends, and changes state with
 * a choreographed transition instead of a page swap:
 * 1. the card dips;
 * 2. the old content shrinks away quickly while the card resizes on the expressive spring;
 * 3. the big number travels into place and Start fasting morphs into End fast;
 * 4. the dial blooms in and draws its progress from empty;
 * 5. supporting text settles in last. Ending a fast plays the same choreography in reverse. On load
 *    the current state just appears.
 *
 * [active] is null when no fast is running.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TodayHeroCard(
  idle: IdleHeroState,
  active: ActiveHero?,
  wide: Boolean,
  dialSize: Dp,
  onStartFasting: () -> Unit,
  onEndFast: () -> Unit,
  onChangeGoal: () -> Unit,
  onEditStart: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val motion = MaterialTheme.motionScheme
  val colors = MaterialTheme.colorScheme
  val haptics = LocalHapticFeedback.current
  val dip = rememberStateChangeDip(isFasting = active != null, motion)

  Card(
    modifier =
      modifier.fillMaxWidth().graphicsLayer {
        scaleX = dip.value
        scaleY = dip.value
      },
    shape = RoundedCornerShape(32.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
      ),
  ) {
    SharedTransitionLayout {
      // Keyed on whether a fast is running: clock ticks update the content in place, and the
      // outgoing content keeps its last state while it animates out.
      AnimatedContent(
        targetState = active,
        contentKey = { it != null },
        transitionSpec = {
          // The pieces carry the motion; the content as a whole only fades, quickly.
          (fadeIn(motion.fastEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec())).using(
            SizeTransform(clip = false) { _, _ -> motion.defaultSpatialSpec<IntSize>() }
          )
        },
        label = "today-hero",
      ) { shown ->
        val heroMotion = rememberHeroMotion(this@SharedTransitionLayout, this, motion)
        if (shown != null) {
          ActiveHeroContent(
            active = shown,
            wide = wide,
            dialSize = dialSize,
            motion = heroMotion,
            onEndFast = {
              haptics.performHapticFeedback(HapticFeedbackType.Confirm)
              onEndFast()
            },
            onEditStart = onEditStart,
            onEditGoal = onChangeGoal,
          )
        } else {
          IdleHeroContent(
            state = idle,
            wide = wide,
            motion = heroMotion,
            onStartFasting = {
              haptics.performHapticFeedback(HapticFeedbackType.Confirm)
              onStartFasting()
            },
            onChangeGoal = onChangeGoal,
          )
        }
      }
    }
  }
}

/** A small label with its icon, such as "Next fast" on the idle card or "Fasting" in the dial. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun Eyebrow(text: String, @DrawableRes icon: Int, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      painter = painterResource(icon),
      contentDescription = null,
      modifier = Modifier.size(20.dp),
    )
    Text(text, style = MaterialTheme.typography.labelLargeEmphasized)
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun rememberHeroMotion(
  shared: SharedTransitionScope,
  visibility: AnimatedVisibilityScope,
  motion: MotionScheme,
): HeroMotion {
  // Content composed mid-transition starts in PreEnter; content shown on load starts Visible.
  val isEntering = remember { visibility.transition.currentState == EnterExitState.PreEnter }
  val sharedEnter = fadeIn(motion.defaultEffectsSpec())
  val sharedExit = fadeOut(motion.fastEffectsSpec())
  val boundsTransform = BoundsTransform { _, _ -> motion.defaultSpatialSpec() }
  with(shared) {
    with(visibility) {
      return HeroMotion(
        action =
          Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(PrimaryActionKey),
            animatedVisibilityScope = visibility,
            enter = sharedEnter,
            exit = sharedExit,
            boundsTransform = boundsTransform,
          ),
        number =
          Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(HeroNumberKey),
            animatedVisibilityScope = visibility,
            enter = sharedEnter,
            exit = sharedExit,
            boundsTransform = boundsTransform,
            // Text scales between the two sizes; re-measuring it would rewrap mid-morph.
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
          ),
        lead =
          Modifier.animateEnterExit(
            enter =
              scaleIn(motion.fastSpatialSpec(), initialScale = 0.6f) +
                fadeIn(motion.fastEffectsSpec()),
            exit =
              scaleOut(motion.fastSpatialSpec(), targetScale = 0.85f) +
                fadeOut(motion.fastEffectsSpec()),
          ),
        // The slow spring lands after the fast one, staggering the pieces without delays.
        support =
          Modifier.animateEnterExit(
            enter =
              slideInVertically(motion.slowSpatialSpec()) { it / 3 } +
                fadeIn(motion.slowEffectsSpec()),
            exit = fadeOut(motion.fastEffectsSpec()),
          ),
        isEntering = isEntering,
      )
    }
  }
}

/** A short squash of the card when the fasting state changes, not when it first appears. */
@Composable
private fun rememberStateChangeDip(isFasting: Boolean, motion: MotionScheme): Animatable<Float, *> {
  val dip = remember { Animatable(1f) }
  val current by rememberUpdatedState(isFasting)
  LaunchedEffect(Unit) {
    // The first value is the state on load; only later changes dip.
    snapshotFlow { current }
      .drop(1)
      .collectLatest {
        dip.animateTo(0.97f, motion.fastSpatialSpec())
        dip.animateTo(1f, motion.defaultSpatialSpec())
      }
  }
  return dip
}
