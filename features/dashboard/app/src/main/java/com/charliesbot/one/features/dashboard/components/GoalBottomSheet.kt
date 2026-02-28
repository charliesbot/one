package com.charliesbot.one.features.dashboard.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.charliesbot.shared.R
import com.charliesbot.shared.core.components.CustomGoalCard
import com.charliesbot.shared.core.components.GoalSelectionCard
import com.charliesbot.shared.core.components.WheelPicker
import com.charliesbot.shared.core.constants.FastGoal
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import java.util.UUID

private sealed interface SheetView {
    data object GoalSelection : SheetView
    data object CreateCustomGoal : SheetView
    data class EditCustomGoal(val goal: FastGoal) : SheetView
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GoalBottomSheet(
    initialSelectedGoalId: String,
    allGoals: List<FastGoal>,
    onSave: (selectedGoalId: String) -> Unit,
    onSaveCustomGoal: (FastGoal) -> Unit,
    onDeleteCustomGoal: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentView by remember { mutableStateOf<SheetView>(SheetView.GoalSelection) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
    ) {
        AnimatedContent(
            targetState = currentView,
            transitionSpec = {
                val forward = targetState !is SheetView.GoalSelection
                if (forward) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith
                        (slideOutHorizontally { it } + fadeOut())
                }
            },
            label = "GoalSheetNavigation",
        ) { view ->
            when (view) {
                SheetView.GoalSelection -> GoalSelectionView(
                    initialSelectedGoalId = initialSelectedGoalId,
                    allGoals = allGoals,
                    onSave = onSave,
                    onCustomClick = { currentView = SheetView.CreateCustomGoal },
                    onEditCustomGoal = { goal -> currentView = SheetView.EditCustomGoal(goal) },
                )

                SheetView.CreateCustomGoal -> {
                    BackHandler { currentView = SheetView.GoalSelection }
                    CustomGoalFormView(
                        editingGoal = null,
                        onSave = onSaveCustomGoal,
                        onDelete = null,
                    )
                }

                is SheetView.EditCustomGoal -> {
                    BackHandler { currentView = SheetView.GoalSelection }
                    CustomGoalFormView(
                        editingGoal = view.goal,
                        onSave = onSaveCustomGoal,
                        onDelete = onDeleteCustomGoal,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalSelectionView(
    initialSelectedGoalId: String,
    allGoals: List<FastGoal>,
    onSave: (selectedGoalId: String) -> Unit,
    onCustomClick: () -> Unit,
    onEditCustomGoal: (FastGoal) -> Unit,
) {
    var temporarilySelectedId by remember(initialSelectedGoalId) {
        mutableStateOf(initialSelectedGoalId)
    }

    Column(
        modifier = Modifier.padding(vertical = 12.dp, horizontal = 36.dp),
    ) {
        Text(
            text = stringResource(R.string.select_goal),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(allGoals.size) { index ->
                val goal = allGoals[index]
                GoalSelectionCard(
                    goal = goal,
                    isSelected = goal.id == temporarilySelectedId,
                    onClick = { temporarilySelectedId = goal.id },
                    onEdit = { onEditCustomGoal(goal) },
                )
            }
            item {
                CustomGoalCard(onClick = onCustomClick)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        FilledTonalButton(
            onClick = { onSave(temporarilySelectedId) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.save))
        }
    }
}

@Composable
private fun CustomGoalFormView(editingGoal: FastGoal?, onSave: (FastGoal) -> Unit, onDelete: ((String) -> Unit)?) {
    val isEditing = editingGoal != null
    val days = remember { (0..7).toList() }
    val hours = remember { (0..23).toList() }
    val colors = remember { PredefinedFastingGoals.customGoalColors }

    val initialTotalHours = editingGoal?.let { (it.durationMillis / (60L * 60L * 1000L)).toInt() } ?: 16
    val initialDays = initialTotalHours / 24
    val initialHours = initialTotalHours % 24

    var selectedDays by remember { mutableIntStateOf(initialDays) }
    var selectedHours by remember { mutableIntStateOf(initialHours) }
    var goalName by remember { mutableStateOf(editingGoal?.titleText ?: "") }
    var selectedColor by remember { mutableStateOf(editingGoal?.color ?: colors.first()) }

    val totalHours = selectedDays * 24 + selectedHours
    val isValid = totalHours > 0 && goalName.isNotBlank()

    Column(
        modifier = Modifier.padding(vertical = 12.dp, horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = stringResource(if (isEditing) R.string.edit_goal else R.string.custom_fast),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        // Duration pickers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Days picker
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.custom_goal_days),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                WheelPicker(
                    items = days,
                    initialIndex = initialDays,
                    onSelectedIndexChange = { selectedDays = days[it] },
                    visibleItemCount = 3,
                    modifier = Modifier.width(80.dp),
                ) { item ->
                    Text(
                        text = item.toString(),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.width(32.dp))

            // Hours picker
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.custom_goal_hours),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                WheelPicker(
                    items = hours,
                    initialIndex = initialHours,
                    onSelectedIndexChange = { selectedHours = hours[it] },
                    visibleItemCount = 3,
                    modifier = Modifier.width(80.dp),
                ) { item ->
                    Text(
                        text = item.toString(),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }

        // Goal name input
        OutlinedTextField(
            value = goalName,
            onValueChange = { goalName = it },
            label = { Text(stringResource(R.string.custom_goal_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // Color picker
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            colors.forEach { color ->
                val isColorSelected = color == selectedColor
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.8f), CircleShape)
                        .then(
                            if (isColorSelected) {
                                Modifier.border(
                                    BorderStroke(3.dp, MaterialTheme.colorScheme.onBackground),
                                    CircleShape,
                                )
                            } else {
                                Modifier
                            },
                        )
                        .clickable { selectedColor = color },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isEditing && onDelete != null) {
                OutlinedButton(
                    onClick = { onDelete(editingGoal.id) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.delete_goal))
                }
            }
            FilledTonalButton(
                onClick = {
                    val goal = FastGoal(
                        id = editingGoal?.id ?: "custom_${UUID.randomUUID()}",
                        titleText = goalName.trim(),
                        durationDisplay = totalHours.toString(),
                        color = selectedColor,
                        durationMillis = totalHours * 60L * 60L * 1000L,
                    )
                    onSave(goal)
                },
                enabled = isValid,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(if (isEditing) R.string.update_goal else R.string.save_goal))
            }
        }
    }
}
