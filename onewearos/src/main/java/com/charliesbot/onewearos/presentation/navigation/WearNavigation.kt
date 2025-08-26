package com.charliesbot.onewearos.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.charliesbot.onewearos.presentation.feature.today.WearGoalOptionsScreen
import com.charliesbot.onewearos.presentation.feature.today.WearDatePickerScreen
import com.charliesbot.onewearos.presentation.feature.today.WearStartDateScreen
import com.charliesbot.onewearos.presentation.feature.today.WearTimePickerScreen
import com.charliesbot.onewearos.presentation.theme.OneTheme
import com.charliesbot.onewearos.presentation.feature.today.WearTodayScreen

@Composable
fun WearNavigation() {
    val navController = rememberSwipeDismissableNavController()
    OneTheme {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = WearNavigationRoute.Today.route
        ) {
            composable(WearNavigationRoute.Today.route) {
                WearTodayScreen(
                    onNavigateToStartDateSelection = {
                        navController.navigate(WearNavigationRoute.StartDateSelection.route)
                    },
                    onNavigateToGoalSelection = {
                        navController.navigate(WearNavigationRoute.GoalOptions.route)
                    }
                )
            }
            composable(WearNavigationRoute.StartDateSelection.route) {
                WearStartDateScreen(navController = navController)
            }
            composable(WearNavigationRoute.GoalOptions.route) {
                WearGoalOptionsScreen(navController = navController)
            }
            composable(WearNavigationRoute.DatePicker.route) {
                WearDatePickerScreen(navController = navController)
            }
            composable(WearNavigationRoute.TimePicker.route) {
                WearTimePickerScreen(navController = navController)
            }
        }
    }
}