package com.charliesbot.onewearos

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.charliesbot.shared.R
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun WearGoalOptionsScreen(viewModel: WearTodayViewModel = koinViewModel(), navController: NavController) {
    val scope = rememberCoroutineScope()
    val fastGoals by viewModel.allGoals.collectAsStateWithLifecycle()
    val columnState = rememberTransformingLazyColumnState()
    val contentPadding = rememberResponsiveColumnPadding(
        first = ColumnItemType.ListHeader,
        last = ColumnItemType.Button,
    )
    val transformationSpec = rememberTransformationSpec()
    ScreenScaffold(scrollState = columnState, contentPadding = contentPadding) {
        TransformingLazyColumn(state = columnState, contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(
                        text = stringResource(R.string.select_goal),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            items(items = fastGoals) { goal ->
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = goal.color.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),

                    onClick = {
                        scope.launch {
                            viewModel.updateFastingGoal(goal.id)
                            navController.popBackStack()
                        }
                    },
                ) {
                    val hours = stringResource(R.string.hours)
                    val label = if (goal.titleText != null) {
                        "${goal.titleText} ${goal.durationDisplay} $hours"
                    } else {
                        "${goal.durationDisplay} $hours"
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// @Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
// @Composable
// private fun DefaultPreview() {
//    WearGoalOptionsScreen(
//
//    )
// }
