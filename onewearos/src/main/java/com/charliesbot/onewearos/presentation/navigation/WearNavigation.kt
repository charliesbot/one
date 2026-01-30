package com.charliesbot.onewearos.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.charliesbot.onewearos.presentation.feature.today.WearDatePickerScreen
import com.charliesbot.onewearos.presentation.feature.today.WearFastingOptionsScreen
import com.charliesbot.onewearos.presentation.feature.today.WearGoalOptionsScreen
import com.charliesbot.onewearos.presentation.feature.today.WearStartDateScreen
import com.charliesbot.onewearos.presentation.feature.today.WearTimePickerScreen
import com.charliesbot.onewearos.presentation.feature.today.WearTodayScreen
import com.charliesbot.onewearos.presentation.feature.today.WearTodayViewModel
import com.charliesbot.onewearos.presentation.theme.OneTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun WearNavigation() {
    val navController = rememberSwipeDismissableNavController()
    val wearTodayViewModel: WearTodayViewModel = koinViewModel()

    OneTheme {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = WearNavigationRoute.Today.route
        ) {
            composable(WearNavigationRoute.Today.route) {
                WearTodayScreen(
                    viewModel = wearTodayViewModel,
                    onNavigateToStartDateSelection = {
                        navController.navigate(WearNavigationRoute.StartDateSelection.route)
                    },
                    onNavigateToGoalSelection = {
                        navController.navigate(WearNavigationRoute.GoalOptions.route)
                    },
                    onNavigateToFastingOptions = {
                        navController.navigate(WearNavigationRoute.FastingOptions.route)
                    }
                )
            }
            composable(WearNavigationRoute.FastingOptions.route) {
                WearFastingOptionsScreen(
                    navController = navController,
                    viewModel = wearTodayViewModel
                )
            }
            composable(WearNavigationRoute.StartDateSelection.route) {
                WearStartDateScreen(
                    navController = navController,
                    viewModel = wearTodayViewModel
                )
            }
            composable(WearNavigationRoute.DatePicker.route) {
                WearDatePickerScreen(
                    navController = navController,
                    viewModel = wearTodayViewModel
                )
            }
            composable(WearNavigationRoute.TimePicker.route) {
                WearTimePickerScreen(
                    navController = navController,
                    viewModel = wearTodayViewModel
                )
            }
            composable(WearNavigationRoute.GoalOptions.route) {
                WearGoalOptionsScreen(
                    navController = navController,
                    viewModel = wearTodayViewModel
                )
            }
        }
    }
}