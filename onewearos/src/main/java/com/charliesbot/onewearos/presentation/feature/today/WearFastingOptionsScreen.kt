package com.charliesbot.onewearos.presentation.feature.today

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.tooling.preview.devices.WearDevices
import com.charliesbot.onewearos.R
import com.charliesbot.onewearos.presentation.navigation.WearNavigationRoute
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import org.koin.androidx.compose.koinViewModel
import com.charliesbot.shared.R as SharedR

@Composable
fun WearFastingOptionsScreen(
    navController: NavController,
    viewModel: WearTodayViewModel = koinViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.initializeTemporalTime()
    }

    WearFastingOptionsContent(
        onNavigateToStartDateSelection = {
            navController.navigate(WearNavigationRoute.StartDateSelection.route)
        },
        onNavigateToGoalSelection = {
            navController.navigate(WearNavigationRoute.GoalOptions.route)
        }
    )
}

@Composable
fun WearFastingOptionsContent(
    onNavigateToStartDateSelection: () -> Unit,
    onNavigateToGoalSelection: () -> Unit,
) {
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
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text(
                        text = stringResource(R.string.fasting_options_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            item {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    onClick = onNavigateToStartDateSelection,
                ) {
                    Text(
                        text = stringResource(SharedR.string.wheel_picker_update),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            item {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    onClick = onNavigateToGoalSelection,
                ) {
                    Text(
                        text = stringResource(R.string.update_goal),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
private fun WearFastingOptionsPreview() {
    WearFastingOptionsContent(
        onNavigateToStartDateSelection = {},
        onNavigateToGoalSelection = {}
    )
}
