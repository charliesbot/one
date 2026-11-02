package com.charliesbot.one.features.dashboard.model

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * The first moment at or after [fromMillis] whose local time in [zone] is [minutesOfDay] minutes
 * past midnight. A time skipped by a daylight saving jump resolves to the shifted local time.
 */
internal fun nextTimeOfDay(minutesOfDay: Int, fromMillis: Long, zone: ZoneId): Long {
  val from = Instant.ofEpochMilli(fromMillis).atZone(zone)
  val sameDay = from.with(LocalTime.of(minutesOfDay / 60, minutesOfDay % 60))
  val next = if (sameDay.isBefore(from)) sameDay.plusDays(1) else sameDay
  return next.toInstant().toEpochMilli()
}

private val SuggestedStartGraceMillis = TimeUnit.HOURS.toMillis(1)

/**
 * When the suggested fast starts, from its time of day. Today's suggestion stays current for an
 * hour after it passes, so the card reads "now" instead of jumping to tomorrow the minute it is
 * due; after that the next day's suggestion takes over.
 */
internal fun suggestedStartMillis(minutesOfDay: Int, nowMillis: Long, zone: ZoneId): Long =
  nextTimeOfDay(minutesOfDay, nowMillis - SuggestedStartGraceMillis, zone)
