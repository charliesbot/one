package com.charliesbot.one.navigation

import kotlinx.serialization.Serializable

sealed interface NavigationRoute

@Serializable
data object Home : NavigationRoute