package com.charliesbot.one.features.dashboard.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

private const val PrimaryActionKey = "today-primary-action"
private const val HeroNumberKey = "today-hero-number"

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
) {
  companion object {
    val None = HeroMotion(Modifier, Modifier, Modifier, Modifier, isEntering = false)
  }
}

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
 * [active] is the last known active content; it stays set briefly after a fast ends so the outgoing
 * content can animate out.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TodayHeroCard(
  isFasting: Boolean,
  idle: IdleHeroState,
  active: ActiveHeroState?,
  details: FastDetailsState?,
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
  val dip = rememberStateChangeDip(isFasting, motion)

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
      AnimatedContent(
        targetState = isFasting && active != null && details != null,
        transitionSpec = {
          // The pieces carry the motion; the content as a whole only fades, quickly.
          (fadeIn(motion.fastEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec())).using(
            SizeTransform(clip = false) { _, _ -> motion.defaultSpatialSpec<IntSize>() }
          )
        },
        label = "today-hero",
      ) { showActive ->
        val heroMotion = rememberHeroMotion(this@SharedTransitionLayout, this, motion)
        if (showActive && active != null && details != null) {
          ActiveHeroContent(
            state = active,
            details = details,
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun rememberHeroMotion(
  shared: SharedTransitionScope,
  visibility: AnimatedVisibilityScope,
  motion: MotionScheme,
): HeroMotion {
  // Content composed mid-transition starts in PreEnter; content shown on load starts Visible.
  val isEntering = remember { visibility.transition.currentState == EnterExitState.PreEnter }
  with(shared) {
    with(visibility) {
      val sharedFade = fadeIn(motion.defaultEffectsSpec()) to fadeOut(motion.fastEffectsSpec())
      return HeroMotion(
        action =
          Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(PrimaryActionKey),
            animatedVisibilityScope = visibility,
            enter = sharedFade.first,
            exit = sharedFade.second,
            boundsTransform = { _: Rect, _: Rect -> motion.defaultSpatialSpec() },
          ),
        number =
          Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(HeroNumberKey),
            animatedVisibilityScope = visibility,
            enter = sharedFade.first,
            exit = sharedFade.second,
            boundsTransform = { _: Rect, _: Rect -> motion.defaultSpatialSpec() },
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
  val lastState = remember { BooleanHolder(isFasting) }
  LaunchedEffect(isFasting) {
    if (lastState.value != isFasting) {
      lastState.value = isFasting
      dip.animateTo(0.97f, motion.fastSpatialSpec())
      dip.animateTo(1f, motion.defaultSpatialSpec())
    }
  }
  return dip
}

private class BooleanHolder(var value: Boolean)
