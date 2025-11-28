package com.charliesbot.one.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import com.charliesbot.shared.R
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.charliesbot.one.features.dashboard.TodayScreen
import com.charliesbot.one.features.profile.YouScreen
import com.charliesbot.one.features.settings.SettingsScreen

@ExperimentalMaterial3ExpressiveApi
@Composable
fun MainNavigation() {
    val backStack = remember { mutableStateListOf<NavigationRoute>(Today) }
    val currentDestination = backStack.lastOrNull() ?: Today

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.today_24px),
                            contentDescription = stringResource(R.string.nav_today)
                        )
                    },
                    label = { Text(stringResource(R.string.nav_today)) },
                    selected = currentDestination == Today,
                    onClick = {
                        if (currentDestination != Today) {
                            backStack.clear()
                            backStack.add(Today)
                        }
                    }
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            painterResource(R.drawable.person_24px),
                            contentDescription = stringResource(R.string.nav_you)
                        )
                    },
                    label = { Text(stringResource(R.string.nav_you)) },
                    selected = currentDestination == You,
                    onClick = {
                        if (currentDestination != You) {
                            backStack.clear()
                            backStack.add(You)
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<Today> {
                    TodayScreen()
                }
                entry<You> {
                    YouScreen(
                        onNavigateToSettings = {
                            backStack.add(Settings)
                        }
                    )
                }
                entry<Settings> {
                    SettingsScreen(
                        onNavigateBack = {
                            backStack.removeLastOrNull()
                        }
                    )
                }
            },
            modifier = Modifier.padding(paddingValues)
        )
    }
}