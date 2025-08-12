package com.charliesbot.onewearos.presentation.navigation

sealed class WearNavigationRoute(val route: String) {
    data object Today : WearNavigationRoute("today")
    data object GoalOptions : WearNavigationRoute("goal_options")
}