package com.charliesbot.onewearos.presentation.feature.today

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.DatePicker
import androidx.navigation.NavController
import com.charliesbot.shared.core.utils.convertMillisToLocalDateTime
import org.koin.androidx.compose.koinViewModel

@Composable
fun WearDatePickerScreen(
    navController: NavController,
    viewModel: WearStartDateViewModel = koinViewModel()
) {
    val startTimeInMillis by viewModel.startTimeInMillis.collectAsStateWithLifecycle()
    val initialDate = convertMillisToLocalDateTime(startTimeInMillis).toLocalDate()

    DatePicker(
        initialDate = initialDate,
        onDatePicked = { newDate ->
            viewModel.updateStartDate(newDate)
            navController.popBackStack()
        }
    )
}