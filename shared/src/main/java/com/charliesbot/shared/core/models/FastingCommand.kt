package com.charliesbot.shared.core.models

enum class FastingCommand(val path: String) {
    START_FASTING("/start_fasting"),
    STOP_FASTING("/stop_fasting"),
    UPDATE_START_TIME("/update_start_time")
}