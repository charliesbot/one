package com.charliesbot.shared.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.charliesbot.shared.R
import com.charliesbot.shared.core.constants.FastGoal


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SelectionCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isSelected: Boolean,
    title: String,
    description: String,
    color: Color,
    onEdit: (() -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        border = if (isSelected) BorderStroke(
            3.dp,
            MaterialTheme.colorScheme.onBackground
        ) else null,
        colors = CardDefaults.cardColors(
            containerColor = color
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                minLines = 2,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.hours))
                Spacer(modifier = Modifier.weight(1f))
                if (onEdit != null) {
                    OutlinedIconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.edit_24px),
                            contentDescription = "",
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSelectionCard(
    modifier: Modifier = Modifier,
    goal: FastGoal,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
) {
    SelectionCard(
        modifier = modifier,
        onClick = onClick,
        isSelected = isSelected,
        title = goal.getTitle(LocalContext.current),
        description = goal.durationDisplay,
        color = goal.color.copy(alpha = 0.8f),
        onEdit = if (goal.isCustom) onEdit else null,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomGoalCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    SelectionCard(
        modifier = modifier,
        onClick = onClick,
        isSelected = false,
        title = stringResource(R.string.custom_fast),
        description = "+",
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
}
