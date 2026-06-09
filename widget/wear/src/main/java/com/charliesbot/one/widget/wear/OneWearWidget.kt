package com.charliesbot.one.widget.wear

import android.content.Context
import androidx.compose.remote.creation.compose.layout.RemoteColumn
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
    RemoteColumn {
      RemoteText(text = RemoteString(content.primaryText))
      RemoteText(text = RemoteString(content.secondaryText))
    }
  }
}

private fun defaultFastingData() =
  FastingDataItem(fastingGoalId = FastingGoalCatalog.DEFAULT_GOAL_ID)
