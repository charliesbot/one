package com.charliesbot.onewearos.presentation.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.charliesbot.shared.core.components.FastingProgressBar
import com.charliesbot.shared.core.components.TimeInfoDisplay
import com.charliesbot.shared.core.utils.calculateProgressFraction
import com.charliesbot.shared.core.utils.convertMillisToLocalDateTime
import com.charliesbot.shared.core.utils.formatTimestamp
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel


@Composable
fun WearTodayScreen(viewModel: WearTodayViewModel = koinViewModel()) {
    var elapsedTime by remember { mutableLongStateOf(0L) }
    val startTimeInMillis by viewModel.startTimeInMillis.collectAsStateWithLifecycle()
    val startTimeInLocalDateTime =
        convertMillisToLocalDateTime(startTimeInMillis)
    val isFasting by viewModel.isFasting.collectAsStateWithLifecycle()
    val fastButtonLabel = if (isFasting) "End Fast" else "Start Fasting"

    val timeLabel = if (isFasting) {
        formatTimestamp(elapsedTime)
    } else {
        "16 hours"
    }
    LaunchedEffect(isFasting) {
        if (isFasting) {
            while (true) {
                elapsedTime = System.currentTimeMillis() - startTimeInMillis
                delay(1000L) // refresh timer every second
            }
        } else {
            elapsedTime = 0L
        }
    }
    Scaffold {
        FastingProgressBar(
            progress = calculateProgressFraction(elapsedTime),
            strokeWidth = 8.dp,
            indicatorColor = MaterialTheme.colors.primary,
            trackColor = MaterialTheme.colors.onBackground,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = timeLabel,
                    color = MaterialTheme.colors.onBackground,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(15.dp))
                AnimatedVisibility(
                    visible = isFasting
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
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
                }
                Spacer(Modifier.height(15.dp))
                Button(
                    onClick = if (isFasting) viewModel::onStopFasting else viewModel::onStartFasting,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(fastButtonLabel)
                }
            }
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearTodayScreen()
}
