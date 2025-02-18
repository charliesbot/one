package com.charliesbot.one.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.charliesbot.one.core.components.FastingTimeAction
import com.charliesbot.one.core.components.WeeklyProgress
import com.charliesbot.one.today.components.CurrentFastingProgress
import com.charliesbot.one.ui.theme.OneTheme
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDateTime

@Composable
fun TodayScreen(viewModel: TodayViewModel = koinViewModel()) {
    val screenPadding = 32.dp
    val isFasting by viewModel.isFasting.collectAsState()
    val starTimeInMillis by viewModel.startTimeInMillis.collectAsState()
    var elapsedTime by remember { mutableLongStateOf(0L) }
    val fastButtonLabel = if (isFasting) "End Fast" else "Start Fasting"

    LaunchedEffect(isFasting) {
        if (isFasting) {
            while (true) {
                elapsedTime = System.currentTimeMillis() - starTimeInMillis
                delay(1000L) // refresh timer every second
            }
        } else {
            elapsedTime = 0L
        }
    }

    Scaffold() { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WeeklyProgress(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth()
                    .padding(horizontal = screenPadding + 24.dp)
            )
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(600.dp)
                    .padding(screenPadding),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp,
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(all = 32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    CurrentFastingProgress(isFasting = isFasting, elapsedTime = elapsedTime)
                    if (isFasting) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            FastingTimeAction(title = "Started", date = LocalDateTime.now())
                            FastingTimeAction(
                                title = "Goal",
                                date = LocalDateTime.now().plusHours(16)
                            )
                        }
                    }
                    FilledTonalButton(
                        onClick = if (isFasting) viewModel::onStopFasting else viewModel::onStartFasting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(text = fastButtonLabel)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTodayScreen() {
    OneTheme {
        TodayScreen()
    }
}