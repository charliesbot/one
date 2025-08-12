package com.charliesbot.shared.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.charliesbot.shared.R
import com.charliesbot.shared.core.constants.FastGoal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSelectionCard(
    goal: FastGoal,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        border = if (isSelected) BorderStroke(
            2.dp,
            Color.White
        ) else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 2.dp else 1.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = goal.color.copy(alpha = 0.8f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
        ) {
            Text(
                text = goal.getTitle(LocalContext.current),
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = goal.durationDisplay,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(text = stringResource(R.string.hours))
        }
    }
}