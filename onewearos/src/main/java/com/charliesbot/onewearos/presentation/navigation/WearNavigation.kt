package com.charliesbot.onewearos.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.charliesbot.onewearos.presentation.feature.goaloptions.WearGoalOptionsScreen
import com.charliesbot.onewearos.presentation.theme.OneTheme
import com.charliesbot.onewearos.presentation.feature.today.WearTodayScreen

@Composable
fun WearNavigation() {
    val navController = rememberSwipeDismissableNavController()
    OneTheme {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = WearNavigationRoute.GoalOptions.route
        ) {
            composable(WearNavigationRoute.Today.route) {
                WearTodayScreen()
            }
            composable(WearNavigationRoute.GoalOptions.route) {
                WearGoalOptionsScreen()
            }
        }
    }
}