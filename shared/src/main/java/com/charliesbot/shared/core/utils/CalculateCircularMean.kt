package com.charliesbot.shared.core.utils

import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Calculate the circular mean of LocalTime values.
 * 
 * This handles the wrap-around problem where averaging times like 23:30 and 00:30
 * should give 00:00 (midnight), not 12:00 (noon) as a simple arithmetic mean would.
 * 
 * Algorithm:
 * 1. Convert each time to an angle on a unit circle (0-2π radians for 24 hours)
 * 2. Calculate the mean of the sine and cosine components
 * 3. Use atan2 to convert back to an angle
 * 4. Convert the angle back to a LocalTime
 * 
 * @param times List of LocalTime values to average
 * @return The circular mean as a LocalTime
 * @throws IllegalArgumentException if times list is empty
 */
fun calculateCircularMean(times: List<LocalTime>): LocalTime {
    require(times.isNotEmpty()) { "Cannot calculate circular mean of empty list" }
    
    // Convert each time to an angle (radians)
    val angles = times.map { time ->
        val minutesSinceMidnight = time.toSecondOfDay() / 60.0
        // Map [0, 1440) minutes to [0, 2π) radians
        (minutesSinceMidnight / (24.0 * 60.0)) * 2 * PI
    }
    
    // Calculate mean of sin and cos components
    val sinSum = angles.sumOf { sin(it) }
    val cosSum = angles.sumOf { cos(it) }
    
    // Get the mean angle
    val meanAngle = atan2(sinSum, cosSum)
    
    // Convert back to [0, 2π) range if negative
    val meanAnglePositive = if (meanAngle < 0) meanAngle + 2 * PI else meanAngle
    
    // Convert radians back to minutes since midnight
    val minutesSinceMidnight = ((meanAnglePositive / (2 * PI)) * 24 * 60).toInt()
    
    // Create LocalTime from minutes
    return LocalTime.ofSecondOfDay((minutesSinceMidnight * 60).toLong())
}

