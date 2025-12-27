package com.charliesbot.shared.core.models

/**
 * Represents a calculated suggestion for when the user should start their fast.
 *
 * @property suggestedTimeMillis The absolute timestamp (in UTC milliseconds) when the fast should start.
 * @property suggestedTimeMinutes The time of day as minutes from midnight (0-1439) for display purposes.
 * @property reasoning A human-readable explanation of how this time was calculated.
 * @property source The data source used to calculate this suggestion.
 */
data class SuggestedFastingTime(
    val suggestedTimeMillis: Long,
    val suggestedTimeMinutes: Int,
    val reasoning: String,
    val source: SuggestionSource
)

enum class SuggestionSource {
    /** Calculated from 7-day moving average of user's actual fast start times */
    MOVING_AVERAGE,
    /** Fallback: bedtime minus 5 hours */
    BEDTIME_BASED
}

