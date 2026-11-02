package com.charliesbot.one.features.dashboard.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp

/** A filled Expressive button at one of the standard heights, with a leading icon. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SizedButton(
  text: String,
  @DrawableRes icon: Int,
  height: Dp,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  shapes: ButtonShapes = ButtonDefaults.shapesFor(height),
) {
  Button(
    onClick = onClick,
    shapes = shapes,
    contentPadding = ButtonDefaults.contentPaddingFor(height),
    modifier = modifier.heightIn(min = height),
  ) {
    SizedButtonContent(text, icon, height)
  }
}

/**
 * An elevated Expressive button at one of the standard heights, with a leading icon. Its shadow
 * lifts it off a colored container, where a tonal fill or an outline would blend in.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SizedElevatedButton(
  text: String,
  @DrawableRes icon: Int,
  height: Dp,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ElevatedButton(
    onClick = onClick,
    shapes = ButtonDefaults.shapesFor(height),
    contentPadding = ButtonDefaults.contentPaddingFor(height),
    modifier = modifier.heightIn(min = height),
  ) {
    SizedButtonContent(text, icon, height)
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SizedButtonContent(text: String, @DrawableRes icon: Int, height: Dp) {
  Icon(
    painter = painterResource(icon),
    contentDescription = null,
    modifier = Modifier.size(ButtonDefaults.iconSizeFor(height)),
  )
  Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(height)))
  Text(text = text, style = ButtonDefaults.textStyleFor(height))
}
