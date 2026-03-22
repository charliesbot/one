package com.charliesbot.one.features.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charliesbot.one.features.profile.components.FastingDetailsBottomSheet
import com.charliesbot.shared.R
import com.charliesbot.shared.core.components.FastingMonthCalendar
import org.koin.androidx.compose.koinViewModel

@Composable
fun YouScreen(viewModel: YouViewModel = koinViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  YouScreenContent(
    uiState = uiState,
    onDaySelected = { viewModel.onDaySelected(it) },
    onNextMonth = { viewModel.onNextMonth() },
    onPreviousMonth = { viewModel.onPreviousMonth() },
    onDeleteFastingEntry = { viewModel.onDeleteFastingEntry(it) },
    onUpdateFastingEntry = { original, newStart, newEnd, goalId ->
      viewModel.onUpdateFastingEntry(original, newStart, newEnd, goalId)
    },
  )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun YouScreenContent(
  uiState: CalendarUiState,
  onDaySelected: (com.charliesbot.shared.core.components.FastingDayData?) -> Unit,
  onNextMonth: () -> Unit,
  onPreviousMonth: () -> Unit,
  onDeleteFastingEntry: (Long) -> Unit,
  onUpdateFastingEntry: (Long, Long, Long, String) -> Unit,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        windowInsets = WindowInsets(top = 0.dp),
        title = { Text(stringResource(R.string.nav_you)) },
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxHeight().fillMaxWidth().padding(innerPadding),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Card(
        modifier = Modifier.padding(vertical = 12.dp, horizontal = 20.dp).widthIn(max = 600.dp)
      ) {
        FastingMonthCalendar(
          yearMonth = uiState.selectedMonth,
          fastingData = uiState.fastingData,
          firstDayOfWeek = uiState.firstDayOfWeek,
          onDayClick = { date ->
            val fastingData = uiState.fastingData[date]
            if (fastingData != null) {
              onDaySelected(fastingData)
            }
          },
          onNextMonth = onNextMonth,
          onPreviousMonth = onPreviousMonth,
        )
      }
    }

    // Show bottom sheet when a day is selected
    uiState.selectedDay?.let { selectedDay ->
      FastingDetailsBottomSheet(
        fastingData = selectedDay,
        onDismiss = { onDaySelected(null) },
        onDelete = {
          selectedDay.startTimeEpochMillis?.let { startTime -> onDeleteFastingEntry(startTime) }
        },
        onUpdateStartTime = { newStartTime ->
          selectedDay.startTimeEpochMillis?.let { originalStart ->
            selectedDay.endTimeEpochMillis?.let { endTime ->
              onUpdateFastingEntry(
                originalStart,
                newStartTime,
                endTime,
                selectedDay.goalId ?: "16:8",
              )
            }
          }
        },
        onUpdateEndTime = { newEndTime ->
          selectedDay.startTimeEpochMillis?.let { startTime ->
            onUpdateFastingEntry(startTime, startTime, newEndTime, selectedDay.goalId ?: "16:8")
          }
        },
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun PreviewYouScreen() {
  YouScreenContent(
    uiState = CalendarUiState(),
    onDaySelected = {},
    onNextMonth = {},
    onPreviousMonth = {},
    onDeleteFastingEntry = {},
    onUpdateFastingEntry = { _, _, _, _ -> },
  )
}
