package com.charliesbot.one.features.settings

sealed interface SettingsSideEffect {
  data class ShowSnackbar(val message: String) : SettingsSideEffect
}
