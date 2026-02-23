package com.charliesbot.onewearos.presentation.navigation

sealed class WearNavigationRoute(val route: String) {
    data object Today : WearNavigationRoute("today")
    data object GoalOptions : WearNavigationRoute("goal_options")
    data object StartDateSelection: WearNavigationRoute("start_date_selection")
    data object DatePicker: WearNavigationRoute("date_picker")
    data object TimePicker: WearNavigationRoute("time_picker")
}