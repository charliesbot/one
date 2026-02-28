package com.charliesbot.shared.core.constants

object DataLayerConstants {
    const val FASTING_PATH = "/fasting_state"
    const val IS_FASTING_KEY = "is_fasting"
    const val START_TIME_KEY = "start_time"
    const val UPDATE_TIMESTAMP_KEY = "update_timestamp"
    const val FASTING_GOAL_KEY = "fasting_goal"

    // Smart Reminder sync path
    const val SMART_REMINDER_PATH = "/smart_reminder"
    const val SMART_REMINDER_SUGGESTED_TIME_KEY = "suggested_time"
    const val SMART_REMINDER_REASONING_KEY = "reasoning"
    const val SMART_REMINDER_TIMESTAMP_KEY = "timestamp"

    // Custom Goals sync path
    const val CUSTOM_GOALS_PATH = "/custom_goals"
    const val CUSTOM_GOALS_JSON_KEY = "custom_goals_json"
    const val CUSTOM_GOALS_TIMESTAMP_KEY = "custom_goals_timestamp"
}
