package com.charliesbot.one.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private data class FastGoal(
    val id: String,
    val title: String,
    val duration: String, // e.g., "13", "16-18", "1-168"
    val color: Color,
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalBottomSheet(onDismiss: () -> Unit) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fastGoals = remember {
        listOf(
            FastGoal(
                id = "circadian",
                title = "Circadian\nRhythm TRF",
                duration = "13",
                color = Color(0xFF6096BA) // Dusty Blue / Muted Teal
            ),
            FastGoal(
                id = "16:8",
                title = "16:8\nTRF",
                duration = "16",
                color = Color(0xFF82B387), // Muted Sage Green
            ),
            FastGoal(
                id = "18:6",
                title = "18:6\nTRF",
                duration = "18",
                color = Color(0xFFE5A98C) // Muted Peach / Terracotta
            ),
            FastGoal(
                id = "20:4",
                title = "20:4\nTRF",
                duration = "20",
                color = Color(0xFFC9AB6A) // Muted Gold / Ochre
            ),
            FastGoal(
                id = "36hour",
                title = "36-Hour\nFast",
                duration = "36",
                color = Color(0xFF9787BE) // Dusty Lavender / Muted Plum
            ),
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
    ) {
        Text(
            text = "Select a Goal",
            modifier = Modifier
                .fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(fastGoals.size) { goal ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = fastGoals[goal].color.copy(alpha = 0.8f)
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                    ) {
                        Text(
                            text = fastGoals[goal].title,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = fastGoals[goal].duration,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = "hours")
                    }
                }
            }
        }
    }
}