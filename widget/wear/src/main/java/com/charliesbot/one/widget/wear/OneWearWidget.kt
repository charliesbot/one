@file:SuppressLint("RestrictedApi")

package com.charliesbot.one.widget.wear

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteSpacer
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.glance.wear.GlanceWearWidget
import androidx.glance.wear.WearWidgetBrush
import androidx.glance.wear.WearWidgetData
import androidx.glance.wear.WearWidgetDocument
import androidx.glance.wear.core.WearWidgetParams
import androidx.wear.compose.remote.material3.RemoteMaterialTheme
import androidx.wear.compose.remote.material3.RemoteText
import com.charliesbot.one.widget.common.toFastingWidgetState
import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.models.FastingGoalCatalog
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OneWearWidget : GlanceWearWidget(), KoinComponent {
  private val fastingDataRepository: FastingDataRepository by inject()

  override suspend fun provideWidgetData(
    context: Context,
    params: WearWidgetParams,
  ): WearWidgetData {
    val fastingData = fastingDataRepository.getCurrentFasting() ?: defaultFastingData()
    val goal = FastingGoalCatalog.getGoalById(fastingData.fastingGoalId)
    val state = fastingData.toFastingWidgetState(System.currentTimeMillis(), goal.durationMillis)

    return WearWidgetDocument(background = WearWidgetBrush) {
      OneWearWidgetContent(content = state.toWearWidgetContent(context))
    }
  }
}

@RemoteComposable
@Composable
private fun OneWearWidgetContent(content: WearWidgetContent) {
  RemoteMaterialTheme {
    RemoteBox(
      modifier = RemoteModifier.fillMaxSize().padding(horizontal = 24.rdp, vertical = 18.rdp),
      contentAlignment = RemoteAlignment.Center,
    ) {
      RemoteColumn(horizontalAlignment = RemoteAlignment.CenterHorizontally) {
        RemoteText(
          text = RemoteString(content.primaryText),
          fontSize = 42.rsp,
          color = RemoteMaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold,
          textAlign = TextAlign.Center,
          maxLines = 1,
        )
        if (content.secondaryText.isNotBlank()) {
          RemoteSpacer(modifier = RemoteModifier.height(2.rdp))
          RemoteText(
            text = RemoteString(content.secondaryText),
            fontSize = 18.rsp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
          )
        }
      }
    }
  }
}

private fun defaultFastingData() =
  FastingDataItem(fastingGoalId = FastingGoalCatalog.DEFAULT_GOAL_ID)

private class PreviewOneWearWidget : GlanceWearWidget() {
  override suspend fun provideWidgetData(
    context: Context,
    params: WearWidgetParams,
  ): WearWidgetData =
    WearWidgetDocument(background = WearWidgetBrush) {
      OneWearWidgetContent(
        content = WearWidgetContent(primaryText = "7", secondaryText = "hours left")
      )
    }
}

@Preview
@Composable
private fun OneWearWidgetPreview(
  @PreviewParameter(WearWidgetParamsProviderSnapshot::class) params: WearWidgetParams
) = WearWidgetPreviewSnapshot(PreviewOneWearWidget(), params)
