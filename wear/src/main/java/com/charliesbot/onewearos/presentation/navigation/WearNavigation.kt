package com.charliesbot.onewearos.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.charliesbot.onewearos.WearDatePickerScreen
import com.charliesbot.onewearos.WearGoalOptionsScreen
import com.charliesbot.onewearos.WearStartDateScreen
import com.charliesbot.onewearos.WearTimePickerScreen
import com.charliesbot.onewearos.WearTodayScreen
import com.charliesbot.onewearos.WearTodayViewModel
import com.charliesbot.onewearos.presentation.theme.OneTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun WearNavigation() {
  val navController = rememberSwipeDismissableNavController()
  val wearTodayViewModel: WearTodayViewModel = koinViewModel()

  OneTheme {
    SwipeDismissableNavHost(
      navController = navController,
      startDestination = WearNavigationRoute.Today.route,
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
        )
      }
      composable(WearNavigationRoute.StartDateSelection.route) {
        WearStartDateScreen(
          navController = navController,
          viewModel = wearTodayViewModel,
          onNavigateToDatePicker = { navController.navigate(WearNavigationRoute.DatePicker.route) },
          onNavigateToTimePicker = { navController.navigate(WearNavigationRoute.TimePicker.route) },
        )
      }
      composable(WearNavigationRoute.DatePicker.route) {
        WearDatePickerScreen(navController = navController, viewModel = wearTodayViewModel)
      }
      composable(WearNavigationRoute.TimePicker.route) {
        WearTimePickerScreen(navController = navController, viewModel = wearTodayViewModel)
      }
      composable(WearNavigationRoute.GoalOptions.route) {
        WearGoalOptionsScreen(navController = navController, viewModel = wearTodayViewModel)
      }
    }
  }
}
