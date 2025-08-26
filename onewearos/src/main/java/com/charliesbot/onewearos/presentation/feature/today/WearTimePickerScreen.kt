package com.charliesbot.onewearos.presentation.feature.today

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.TimePicker
import androidx.navigation.NavController
import com.charliesbot.shared.core.utils.convertMillisToLocalDateTime
import org.koin.androidx.compose.koinViewModel

@Composable
fun WearTimePickerScreen(
    navController: NavController,
    viewModel: WearStartDateViewModel = koinViewModel()
) {
    val startTimeInMillis by viewModel.startTimeInMillis.collectAsStateWithLifecycle()
    val initialTime = convertMillisToLocalDateTime(startTimeInMillis).toLocalTime()

    TimePicker(
        initialTime = initialTime,
        onTimePicked = { newTime ->
            viewModel.updateStartTime(newTime)
            navController.popBackStack()
        }
    )
}