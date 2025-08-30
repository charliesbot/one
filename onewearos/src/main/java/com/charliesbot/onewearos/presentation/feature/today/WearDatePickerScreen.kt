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
    viewModel: WearTodayViewModel = koinViewModel()
) {
    val tempStartTime by viewModel.temporalStartTime.collectAsStateWithLifecycle()

    tempStartTime?.let {
        DatePicker(
            initialDate = it.toLocalDate(),
            onDatePicked = { newDate ->
                navController.popBackStack()
            }
        )
    }
}