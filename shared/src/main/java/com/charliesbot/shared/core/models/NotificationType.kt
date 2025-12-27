package com.charliesbot.shared.core.models

enum class NotificationType {
    // Existing fasting progress notifications
    ONE_HOUR_BEFORE,
    COMPLETION,

    // Smart Reminder notifications (pre-fast)
    /** "One hour left to eat!" - triggers 1 hour before suggested start */
    SMART_REMINDER_1H_BEFORE,
    /** "Time to start your fast!" - triggers at suggested start time */
    SMART_REMINDER_START
}
