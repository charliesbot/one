package com.charliesbot.onewearos.presentation.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.charliesbot.onewearos.presentation.navigation.WearNavigationRoute
import com.charliesbot.shared.core.utils.TimeFormat
import com.charliesbot.shared.core.utils.convertMillisToLocalDateTime
import com.charliesbot.shared.core.utils.formatDate
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDateTime

@Composable
fun WearStartDateScreen(
    navController: NavController,
    viewModel: WearStartDateViewModel = koinViewModel(),
) {
    val startTimeInMillis by viewModel.startTimeInMillis.collectAsStateWithLifecycle()
    val startTime = convertMillisToLocalDateTime(startTimeInMillis)

    WearStartDateContent(
        startTime = startTime,
        onNavigateToDatePicker = {
            navController.navigate(WearNavigationRoute.DatePicker.route)
        },
        onNavigateToTimePicker = {
            navController.navigate(WearNavigationRoute.TimePicker.route)
        }
    )
}

@Composable
fun WearStartDateContent(
    startTime: LocalDateTime,
    onNavigateToDatePicker: () -> Unit,
    onNavigateToTimePicker: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(
        scrollState = listState,
        edgeButtonSpacing = 0.dp,
        edgeButton = {
            EdgeButton(
                onClick = { /* ... */ },
                buttonSize = EdgeButtonSize.Small
            ) {
                Text(stringResource(com.charliesbot.shared.R.string.label_save))
            }
        }
    ) { contentPadding ->
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            autoCentering = null,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            item {
                Chip(
                    onClick = onNavigateToDatePicker,
                    label = {
                        Text(
                            text = formatDate(startTime, TimeFormat.DATE),
                            textAlign = TextAlign.Center
                        )
                    },
                    colors = ChipDefaults.gradientBackgroundChipColors()
                )
            }
            item {
                Chip(
                    onClick = onNavigateToTimePicker,
                    label = {
                        Text(
                            text = formatDate(startTime, TimeFormat.TIME),
                            textAlign = TextAlign.Center
                        )
                    },
                    colors = ChipDefaults.gradientBackgroundChipColors()
                )
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun Preview() {
    WearStartDateContent(
        startTime = LocalDateTime.now(),
        onNavigateToDatePicker = {},
        onNavigateToTimePicker = {}
    )
}