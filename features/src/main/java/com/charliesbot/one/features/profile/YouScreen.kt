package com.charliesbot.one.features.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charliesbot.one.features.profile.components.FastingDetailsBottomSheet
import com.charliesbot.shared.R
import com.charliesbot.shared.core.components.FastingMonthCalendar
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun YouScreen(
    viewModel: YouViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_you)) },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(modifier = Modifier.padding(vertical = 12.dp, horizontal = 20.dp)) {
                FastingMonthCalendar(
                    yearMonth = uiState.selectedMonth,
                    fastingData = uiState.fastingData,
                    firstDayOfWeek = uiState.firstDayOfWeek,
                    onDayClick = { date ->
                        val fastingData = uiState.fastingData[date]
                        if (fastingData != null) {
                            viewModel.onDaySelected(fastingData)
                        }
                    },
                    onNextMonth = {
                        viewModel.onNextMonth()
                    },
                    onPreviousMonth = {
                        viewModel.onPreviousMonth()
                    }
                )
            }
        }

        // Show bottom sheet when a day is selected
        uiState.selectedDay?.let { selectedDay ->
            FastingDetailsBottomSheet(
                fastingData = selectedDay,
                onDismiss = {
                    viewModel.onDaySelected(null)
                }
            )
        }
    }
}
