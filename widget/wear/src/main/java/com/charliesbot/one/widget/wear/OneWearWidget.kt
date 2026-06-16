@file:SuppressLint("RestrictedApi")

package com.charliesbot.one.widget.wear

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.remote.creation.compose.action.pendingIntentAction
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteSpacer
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
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
      OneWearWidgetContent(
        content = state.toWearWidgetContent(context),
        openAppPendingIntent = createOpenAppPendingIntent(context),
      )
    }
  }
}

@RemoteComposable
@Composable
private fun OneWearWidgetContent(content: WearWidgetContent, openAppPendingIntent: PendingIntent) {
  RemoteMaterialTheme {
    RemoteBox(
      modifier =
        RemoteModifier.fillMaxSize()
          .clickable(pendingIntentAction(openAppPendingIntent))
          .padding(horizontal = 24.rdp, vertical = 18.rdp),
      contentAlignment = RemoteAlignment.Center,
    ) {
      when (content) {
        is WearWidgetContent.Fasting -> FastingContent(content)
        is WearWidgetContent.NotFasting -> NotFastingContent(content)
      }
    }
  }
}

@RemoteComposable
@Composable
private fun FastingContent(content: WearWidgetContent.Fasting) {
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

@RemoteComposable
@Composable
private fun NotFastingContent(content: WearWidgetContent.NotFasting) {
  RemoteBox(modifier = RemoteModifier.fillMaxSize(), contentAlignment = RemoteAlignment.Center) {
    RemoteText(
      text = RemoteString(content.text),
      fontSize = 24.rsp,
      color = RemoteMaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.SemiBold,
      textAlign = TextAlign.Center,
      maxLines = 3,
    )
  }
}

private fun defaultFastingData() =
  FastingDataItem(fastingGoalId = FastingGoalCatalog.DEFAULT_GOAL_ID)

private fun createOpenAppPendingIntent(context: Context): PendingIntent {
  val intent =
    Intent().apply {
      component = ComponentName(context.packageName, WEAR_MAIN_ACTIVITY_CLASS_NAME)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
  return PendingIntent.getActivity(
    context,
    OPEN_APP_REQUEST_CODE,
    intent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
  )
}

private class PreviewOneWearWidget : GlanceWearWidget() {
  override suspend fun provideWidgetData(
    context: Context,
    params: WearWidgetParams,
  ): WearWidgetData =
    WearWidgetDocument(background = WearWidgetBrush) {
      OneWearWidgetContent(
        content = WearWidgetContent.Fasting(primaryText = "7", secondaryText = "hours left"),
        openAppPendingIntent = createOpenAppPendingIntent(context),
      )
    }
}

@Preview
@Composable
private fun OneWearWidgetPreview(
  @PreviewParameter(WearWidgetParamsProviderSnapshot::class) params: WearWidgetParams
) = WearWidgetPreviewSnapshot(PreviewOneWearWidget(), params)

private const val OPEN_APP_REQUEST_CODE = 0
private const val WEAR_MAIN_ACTIVITY_CLASS_NAME =
  "com.charliesbot.onewearos.presentation.MainActivity"
