@file:SuppressLint("RestrictedApi")

package com.charliesbot.one.widget.wear

import android.annotation.SuppressLint
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.WearWidgetParams
import androidx.glance.wear.core.WidgetInstanceId

internal class WearWidgetParamsProviderSnapshot : PreviewParameterProvider<WearWidgetParams> {
  override val values: Sequence<WearWidgetParams> =
    sequenceOf(
      WearWidgetParams(
        instanceId = WidgetInstanceId("widgets", 1),
        containerType = ContainerInfo.CONTAINER_TYPE_SMALL,
        widthDp = 180f,
        heightDp = 60f,
        verticalPaddingDp = 6f,
        horizontalPaddingDp = 8f,
        cornerRadiusDp = 26f,
      ),
      WearWidgetParams(
        instanceId = WidgetInstanceId("widgets", 2),
        containerType = ContainerInfo.CONTAINER_TYPE_LARGE,
        widthDp = 180f,
        heightDp = 82f,
        verticalPaddingDp = 6f,
        horizontalPaddingDp = 8f,
        cornerRadiusDp = 32f,
      ),
    )
}
