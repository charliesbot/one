package com.charliesbot.onewearos.presentation.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.charliesbot.shared.core.components.TimeInfoDisplay
import com.charliesbot.shared.core.utils.convertMillisToLocalDateTime


@Composable
fun TodayScreen() {
    val startTimeInLocalDateTime =
        convertMillisToLocalDateTime(System.currentTimeMillis())
    Scaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                TimeInfoDisplay(
                    title = "Started",
                    date = startTimeInLocalDateTime,
                    isForWear = true
                )
                TimeInfoDisplay(
                    title = "Goal",
                    date = startTimeInLocalDateTime.plusHours(16),
                    isForWear = true
                )
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
            )  {
               Text("Start Fast")
            }
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    TodayScreen()
}
