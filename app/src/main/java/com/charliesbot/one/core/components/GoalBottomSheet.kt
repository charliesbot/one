package com.charliesbot.one.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.charliesbot.one.R
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GoalBottomSheet(
    initialSelectedGoalId: String,
    onSave: (selectedGoalId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var temporarilySelectedId by remember(initialSelectedGoalId) {
        mutableStateOf(initialSelectedGoalId)
    }
    val fastGoals = remember {
        PredefinedFastingGoals.allGoals
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 36.dp),
        ) {
            Text(
                text = stringResource(R.string.select_goal),
                modifier = Modifier
                    .fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(fastGoals.size) { goal ->
                    Card(
                        onClick = {
                            temporarilySelectedId = fastGoals[goal].id
                        },
                        border = if (fastGoals[goal].id == temporarilySelectedId) BorderStroke(
                            2.dp,
                            Color.White
                        ) else null,
                        elevation = CardDefaults.cardElevation(defaultElevation = if (fastGoals[goal].id == temporarilySelectedId) 2.dp else 1.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = fastGoals[goal].color.copy(alpha = 0.8f)
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                        ) {
                            Text(
                                text = fastGoals[goal].getTitle(LocalContext.current),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = fastGoals[goal].durationDisplay,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = stringResource(R.string.hours))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            FilledTonalButton(
                onClick = { onSave(temporarilySelectedId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}