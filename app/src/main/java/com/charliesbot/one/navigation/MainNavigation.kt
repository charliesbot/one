package com.charliesbot.one.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.charliesbot.one.features.dashboard.TodayScreen
import com.charliesbot.one.features.profile.YouScreen
import com.charliesbot.one.features.settings.SettingsScreen
import com.charliesbot.shared.core.designsystem.common.R as DesignSystemR
import com.charliesbot.shared.core.strings.R as SharedR

private data class TopLevelDestination(
  val route: NavigationRoute,
  @param:DrawableRes val icon: Int,
  @param:StringRes val label: Int,
)

private val topLevelDestinations =
  listOf(
    TopLevelDestination(Today, DesignSystemR.drawable.today_24px, SharedR.string.nav_today),
    TopLevelDestination(You, DesignSystemR.drawable.person_24px, SharedR.string.nav_you),
    TopLevelDestination(
      Settings,
      DesignSystemR.drawable.settings_24px,
      SharedR.string.settings_title,
    ),
  )

/**
 * Top-level navigation. The navigation suite picks the Expressive bar for a compact width, a
 * compact height or tabletop posture, and the collapsed rail otherwise. It always opens on Today,
 * which calls [onTodayReady] once its content is composed.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun MainNavigation(onTodayReady: () -> Unit) {
  val backStack = remember { mutableStateListOf<NavigationRoute>(Today) }
  val currentDestination = backStack.lastOrNull() ?: Today
  val navigateTo: (NavigationRoute) -> Unit = { route ->
    if (currentDestination != route) {
      backStack.clear()
      backStack.add(route)
    }
  }
  val navigationSuiteType =
    NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo())

  NavigationSuiteScaffold(
    navigationSuiteType = navigationSuiteType,
    navigationItems = {
      topLevelDestinations.forEach { destination ->
        NavigationSuiteItem(
          navigationSuiteType = navigationSuiteType,
          selected = currentDestination == destination.route,
          onClick = { navigateTo(destination.route) },
          icon = { Icon(painterResource(destination.icon), contentDescription = null) },
          label = { Text(stringResource(destination.label)) },
        )
      }
    },
  ) {
    // The suite consumes the insets its bar or rail covers; this pads the rest (status bar, and
    // the bottom when the rail is showing).
    Box(Modifier.windowInsetsPadding(WindowInsets.systemBars)) {
      NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
          entryProvider {
            entry<Today> { TodayScreen(onContentReady = onTodayReady) }
            entry<You> { YouScreen() }
            entry<Settings> { SettingsScreen() }
          },
      )
    }
  }
}
