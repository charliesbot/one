@file:SuppressLint("RestrictedApi")

package com.charliesbot.one.widget.wear

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.remote.tooling.preview.RemoteDocPreview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.glance.wear.GlanceWearWidget
import androidx.glance.wear.core.WearWidgetParams
import kotlinx.coroutines.runBlocking

@Composable
internal fun WearWidgetPreviewSnapshot(
  widget: GlanceWearWidget,
  params: WearWidgetParams,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val document =
    remember(widget, params, context) {
      runBlocking {
        widget.provideWidgetData(context, params).captureRawContent(context, params).rcDocument
      }
    }

  RemoteDocPreview(
    document,
    modifier = modifier.width(params.widthDp.dp).height(params.heightDp.dp),
  )
}
