package com.charliesbot.onewearos.presentation.feature.today

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.TimePicker
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel

@Composable
fun WearTimePickerScreen(
    navController: NavController,
    viewModel: WearTodayViewModel = koinViewModel()
) {
    val tempStartTime by viewModel.temporalStartTime.collectAsStateWithLifecycle()

    TimePicker(
        initialTime = tempStartTime?.toLocalTime()!!,
        onTimePicked = { newTime ->
            viewModel.updateTemporalTime(newTime)
            navController.popBackStack()
        }
    )
}