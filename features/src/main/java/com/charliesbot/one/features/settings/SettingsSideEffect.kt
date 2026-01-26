package com.charliesbot.one.features.settings

sealed interface SettingsSideEffect {
    data class ShowSnackbar(val message: String) : SettingsSideEffect
}

internal object SettingsStrings {
    const val EXPORT_SUCCESS = "settings_export_success"
    const val EXPORT_ERROR = "settings_export_error"
    const val SYNC_SUCCESS = "settings_sync_success"
    const val SYNC_ERROR = "settings_sync_error"
    const val VERSION_COPIED = "settings_version_copied"
}
