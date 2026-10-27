package com.charliesbot.one.features.dashboard.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon

private val BadgeIconSize = 20.dp

/** An icon on a Material library shape, such as the Sunny "goal reached" mark. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ShapeBadge(
  shape: RoundedPolygon,
  @DrawableRes icon: Int,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
  size: Dp = 40.dp,
) {
  Box(
    modifier = modifier.size(size).clip(shape.toShape()).background(containerColor),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      painter = painterResource(icon),
      contentDescription = null,
      tint = contentColor,
      modifier = Modifier.size(BadgeIconSize),
    )
  }
}

/** The shape that means "goal reached", shared by completed days and the Goal row. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val GoalReachedShape: RoundedPolygon
  get() = MaterialShapes.Sunny

/** The shape for the Started row. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val StartedShape: RoundedPolygon
  get() = MaterialShapes.Cookie9Sided
