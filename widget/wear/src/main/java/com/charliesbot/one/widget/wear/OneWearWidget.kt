package com.charliesbot.one.widget.wear

import android.content.Context
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.runtime.Composable
import androidx.glance.wear.GlanceWearWidget
import androidx.glance.wear.WearWidgetBrush
import androidx.glance.wear.WearWidgetData
import androidx.glance.wear.WearWidgetDocument
import androidx.glance.wear.core.WearWidgetParams
import androidx.wear.compose.remote.material3.RemoteMaterialTheme
import androidx.wear.compose.remote.material3.RemoteText

class OneWearWidget : GlanceWearWidget() {
  override suspend fun provideWidgetData(
    context: Context,
    params: WearWidgetParams,
  ): WearWidgetData =
    WearWidgetDocument(background = WearWidgetBrush) {
      OneWearWidgetContent()
    }
}

@RemoteComposable
@Composable
private fun OneWearWidgetContent() {
  RemoteMaterialTheme {
    RemoteText(text = RemoteString("ONE"))
  }
}
