package com.charliesbot.one.features.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charliesbot.shared.core.components.FastingMonthCalendar
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun YouScreen(viewModel: YouViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
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
                    onDayClick = {},
                    onNextMonth = {
                        viewModel.onNextMonth()
                    },
                    onPreviousMonth = {
                        viewModel.onPreviousMonth()
                    }
                )
            }
        }
    }
}
