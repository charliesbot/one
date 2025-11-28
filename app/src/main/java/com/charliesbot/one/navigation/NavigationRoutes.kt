package com.charliesbot.one.navigation

import kotlinx.serialization.Serializable

sealed interface NavigationRoute

@Serializable
data object Today : NavigationRoute

@Serializable
data object You : NavigationRoute

@Serializable
data object Settings : NavigationRoute