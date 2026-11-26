@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bot.charlies.androiddesign.preview.kit.*

// Today screen redesign explorations. Baseline lives in baseline.explore.kt.
// Data: 16:8 fast started Wed 8:00 PM, 13h 20m elapsed (83%), goal Thu 12:00 PM.

// ---------- Shared data + shell ----------

private data class Day(val label: String, val hours: Int?, val progress: Float, val today: Boolean = false)

private val week =
  listOf(
    Day("M", 17, 1f),
    Day("T", 16, 1f),
    Day("W", 14, 0.85f),
    Day("T", null, 0.83f, today = true),
    Day("F", null, 0f),
    Day("S", null, 0f),
    Day("S", null, 0f),
  )

private val recent =
  listOf("Wed, Nov 25" to "14h 05m", "Tue, Nov 24" to "16h 30m", "Mon, Nov 23" to "17h 12m", "Sun, Nov 22" to "20h 02m")

private val numerals = TextStyle(fontFamily = bundledFontFamily(BundledFont.GoogleSansCode, weight = 500), fontFeatureSettings = "tnum")

@Composable
private fun TodayShell(content: @Composable (wide: Boolean) -> Unit) {
  val tabs = listOf("Today" to "today", "You" to "person", "Settings" to "settings")
  BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
    val wide = maxWidth >= 600.dp
    if (wide) {
      Row(Modifier.fillMaxSize()) {
        WideNavigationRail(state = rememberWideNavigationRailState()) {
          tabs.forEachIndexed { i, (label, icon) ->
            WideNavigationRailItem(
              railExpanded = false,
              selected = i == 0,
              onClick = {},
              icon = { SymbolIcon(icon, filled = i == 0) },
              label = { Text(label) },
            )
          }
        }
        Box(Modifier.weight(1f).fillMaxHeight()) { content(true) }
      }
    } else {
      Scaffold(
        bottomBar = {
          ShortNavigationBar {
            tabs.forEachIndexed { i, (label, icon) ->
              ShortNavigationBarItem(
                selected = i == 0,
                onClick = {},
                icon = { SymbolIcon(icon, filled = i == 0) },
                label = { Text(label) },
              )
            }
          }
        },
      ) { padding -> Box(Modifier.padding(padding)) { content(false) } }
    }
  }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
  Text(
    text,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.onSurface,
    modifier = modifier.semantics { heading() },
  )
}

// ---------- Direction A: Ring, refined ----------
// Evolution of the shipped ring: wavy countdown ring, tabular numerals, equal-height actions,
// and a wide layout that reveals recent fasts instead of stretching the card.

@Composable
private fun RingWeekStrip(modifier: Modifier = Modifier) {
  Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = modifier) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
      week.forEach { d ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            d.label,
            style = if (d.today) MaterialTheme.typography.labelLargeEmphasized else MaterialTheme.typography.labelLarge,
            color = if (d.today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          )
          CircularProgressIndicator(
            progress = { d.progress },
            modifier = Modifier.size(28.dp),
            strokeWidth = 4.dp,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            gapSize = 0.dp,
          )
        }
      }
    }
  }
}

@Composable
private fun RingHero(fasting: Boolean) {
  Box(Modifier.widthIn(max = 320.dp).fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
    CircularWavyProgressIndicator(
      progress = { if (fasting) 0.83f else 0f },
      modifier = Modifier.fillMaxSize(),
      stroke = Stroke(width = with(androidx.compose.ui.platform.LocalDensity.current) { 12.dp.toPx() }),
      trackStroke = Stroke(width = with(androidx.compose.ui.platform.LocalDensity.current) { 12.dp.toPx() }),
      trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(if (fasting) "Fasting · 16:8" else "Next fast", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(
        if (fasting) "13:20:45" else "8:00 PM",
        style = numerals.merge(MaterialTheme.typography.displayMedium),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        if (fasting) "2h 39m to go" else "16h goal",
        style = MaterialTheme.typography.titleMediumEmphasized,
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
private fun RingTimes() {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
    listOf("Started" to "Wed, 8:00 PM", "16h goal" to "Thu, 12:00 PM").forEachIndexed { i, (label, value) ->
      FilledTonalToggleButton(
        checked = false,
        onCheckedChange = {},
        shapes = if (i == 0) ButtonGroupDefaults.connectedLeadingButtonShapes() else ButtonGroupDefaults.connectedTrailingButtonShapes(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier.weight(1f).height(56.dp),
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
          Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        }
      }
    }
  }
}

@Composable
private fun RingSuggestion() {
  Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      SymbolIcon("schedule", tint = MaterialTheme.colorScheme.onSecondaryContainer)
      Text("Suggested from your 7-day average. Bedtime is 10:00 PM.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
  }
}

@Composable
private fun PrimaryAction(fasting: Boolean, modifier: Modifier = Modifier) {
  Button(
    onClick = {},
    shapes = ButtonDefaults.shapesFor(56.dp),
    contentPadding = ButtonDefaults.contentPaddingFor(56.dp),
    modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("ad:primary"),
  ) { Text(if (fasting) "End fast" else "Start fast", style = MaterialTheme.typography.titleMedium) }
}

@Composable
private fun RecentFasts(modifier: Modifier = Modifier) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
    SectionTitle("Recent fasts", Modifier.padding(start = 16.dp, bottom = 8.dp))
    recent.forEachIndexed { i, (day, dur) ->
      SegmentedListItem(
        onClick = {},
        shapes = ListItemDefaults.segmentedShapes(index = i, count = recent.size),
        trailingContent = { Text(dur, style = numerals.merge(MaterialTheme.typography.titleMedium)) },
      ) { Text(day) }
    }
  }
}

@Composable
private fun RingToday(fasting: Boolean) {
  TodayShell { wide ->
    if (wide) {
      Row(Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
          RingHero(fasting)
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
        Column(Modifier.widthIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
          Text("Today", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
          RingWeekStrip()
          if (fasting) RingTimes() else RingSuggestion()
          PrimaryAction(fasting)
          Spacer(Modifier.height(8.dp))
          RecentFasts()
        }
        }
      }
    } else {
      Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Text("Today", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.fillMaxWidth().padding(top = 16.dp).semantics { heading() })
        RingWeekStrip(Modifier.fillMaxWidth())
        RingHero(fasting)
        if (fasting) RingTimes() else RingSuggestion()
        PrimaryAction(fasting)
      }
    }
  }
}

// ---------- Direction B: Countdown numeral ----------
// Type is the structure: elapsed hours set in tall condensed numerals fill the top third,
// a wavy line runs start → goal, and the week becomes a bar chart of hours fasted.

private val tall = TextStyle(fontFamily = bundledFontFamily(BundledFont.BigShouldersDisplay, weight = 800), fontFeatureSettings = "tnum")

@Composable
private fun NumeralHero(fasting: Boolean) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(
      if (fasting) "Fasting since Wed, 8:00 PM" else "Next fast tonight",
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(verticalAlignment = Alignment.Bottom) {
      Text(
        if (fasting) "13" else "8:00",
        style = tall.merge(MaterialTheme.typography.displayLarge).copy(fontSize = if (fasting) 112.sp else 96.sp, lineHeight = if (fasting) 116.sp else 100.sp),
        softWrap = false,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        if (fasting) "h" else "PM",
        style = tall.merge(MaterialTheme.typography.displaySmall),
        softWrap = false,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, end = if (fasting) 12.dp else 0.dp, bottom = 18.dp),
      )
      if (fasting) {
        Text("20", style = tall.merge(MaterialTheme.typography.displayLarge).copy(fontSize = 112.sp, lineHeight = 116.sp), color = MaterialTheme.colorScheme.onSurface)
        Text("m", style = tall.merge(MaterialTheme.typography.displaySmall), color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, bottom = 18.dp))
      }
    }
    if (fasting) {
      LinearWavyProgressIndicator(progress = { 0.83f }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
      Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("8:00 PM", style = numerals.merge(MaterialTheme.typography.labelLarge), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("83%", style = numerals.merge(MaterialTheme.typography.labelLarge), color = MaterialTheme.colorScheme.primary)
        Text("12:00 PM", style = numerals.merge(MaterialTheme.typography.labelLarge), color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    } else {
      Text("Based on your 7-day average · 16h goal", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
private fun MetricTile(label: String, value: String, container: androidx.compose.ui.graphics.Color, content: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
  Surface(onClick = {}, shape = RoundedCornerShape(24.dp), color = container, contentColor = content, modifier = modifier) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(label, style = MaterialTheme.typography.labelLarge)
      Text(value, style = numerals.merge(MaterialTheme.typography.headlineSmall))
    }
  }
}

@Composable
private fun WeekBars(modifier: Modifier = Modifier) {
  Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = modifier) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        SectionTitle("This week")
        Text("2 of 3 goals met", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Row(Modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        week.forEach { d ->
          val h = d.hours ?: if (d.today) 13 else 0
          Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
              Modifier.width(24.dp)
                .height((8 + h * 3).dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                  when {
                    h >= 16 -> MaterialTheme.colorScheme.primary
                    h > 0 -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                  }
                )
            )
            Text(d.label, style = MaterialTheme.typography.labelMedium, color = if (d.today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }
  }
}

@Composable
private fun NumeralActions(fasting: Boolean) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
    PrimaryAction(fasting, Modifier.weight(1f))
    FilledTonalIconButton(onClick = {}, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(56.dp)) {
      SymbolIcon(if (fasting) "edit_calendar" else "tune", contentDescription = if (fasting) "Edit start time" else "Change goal")
    }
  }
}

@Composable
private fun NumeralMetrics() {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    MetricTile("To go", "2h 39m", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, Modifier.weight(1f))
    MetricTile("Goal", "16:8", MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
  }
}

@Composable
private fun NumeralToday(fasting: Boolean) {
  TodayShell { wide ->
    if (wide) {
      Row(Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
          NumeralHero(fasting)
          NumeralActions(fasting)
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
          Column(Modifier.widthIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (fasting) NumeralMetrics()
            WeekBars(Modifier.fillMaxWidth())
            RecentFasts()
          }
        }
      }
    } else {
      Column(Modifier.fillMaxSize().padding(16.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
          NumeralHero(fasting)
          if (fasting) NumeralMetrics()
          WeekBars(Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(16.dp))
        NumeralActions(fasting)
      }
    }
  }
}

// ---------- Direction C: Sunrise dial ----------
// One strong fill: a primaryContainer band holds a Sunny-shaped dial ringed by wavy progress;
// time *remaining* leads in Fraunces. Week below as small sun shapes that fill when the goal is met.

private val soft = TextStyle(fontFamily = bundledFontFamily(BundledFont.Fraunces, weight = 600))

@Composable
private fun SunDial(fasting: Boolean, modifier: Modifier = Modifier) {
  // One role pair in both themes: brown sun / white text (light), peach sun / dark text (dark).
  val sun = MaterialTheme.colorScheme.primary
  val onSun = MaterialTheme.colorScheme.onPrimary
  val onSunVariant = MaterialTheme.colorScheme.onPrimary
  val progress = MaterialTheme.colorScheme.onPrimaryContainer
  val track = MaterialTheme.colorScheme.surfaceBright
  Box(modifier.widthIn(max = 360.dp).fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
    if (fasting) {
      CircularWavyProgressIndicator(
        progress = { 0.83f },
        modifier = Modifier.fillMaxSize(),
        color = progress,
        trackColor = track,
      )
    }
    Box(
      Modifier.fillMaxSize(0.8f).clip(MaterialShapes.Sunny.toShape()).background(sun),
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (fasting) "2h 39m" else "8:00 PM", style = soft.merge(if (fasting) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displaySmall), color = onSun)
        Text(if (fasting) "until your 16h goal" else "suggested start", style = MaterialTheme.typography.bodyLarge, color = onSunVariant)
      }
    }
  }
}

@Composable
private fun SunBand(
  fasting: Boolean,
  modifier: Modifier = Modifier,
  fill: Boolean = false,
  shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(bottomStart = 48.dp, bottomEnd = 48.dp),
) {
  Surface(
    color = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    shape = shape,
    modifier = modifier,
  ) {
    Column((if (fill) Modifier.fillMaxSize() else Modifier).padding(horizontal = 24.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Column(Modifier.fillMaxWidth()) {
        Text("Today", style = soft.merge(MaterialTheme.typography.headlineMedium), modifier = Modifier.semantics { heading() })
        Text(if (fasting) "13h 20m fasted · 83%" else "Rest day so far", style = MaterialTheme.typography.titleMedium)
      }
      if (fill) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { SunDial(fasting) } else SunDial(fasting)
    }
  }
}

@Composable
private fun SunWeek(modifier: Modifier = Modifier) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SectionTitle("This week")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
      week.forEach { d ->
        val met = (d.hours ?: 0) >= 16
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Box(
            Modifier.size(32.dp).clip(MaterialShapes.Sunny.toShape()).background(
              when {
                met -> MaterialTheme.colorScheme.primary
                d.today -> MaterialTheme.colorScheme.primaryContainer
                d.hours != null -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
              }
            ),
            contentAlignment = Alignment.Center,
          ) {
            if (met) SymbolIcon("check", size = 18.dp, tint = MaterialTheme.colorScheme.onPrimary)
          }
          Text(d.label, style = MaterialTheme.typography.labelMedium, color = if (d.today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}

@Composable
private fun SunDetails(fasting: Boolean) {
  val rows =
    if (fasting) listOf(Triple("play_circle", "Started", "Wed, 8:00 PM"), Triple("flag", "Goal · 16:8", "Thu, 12:00 PM"))
    else listOf(Triple("bedtime", "Bedtime", "10:00 PM"), Triple("flag", "Goal", "16:8"))
  Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
    rows.forEachIndexed { i, (icon, label, value) ->
      SegmentedListItem(
        onClick = {},
        shapes = ListItemDefaults.segmentedShapes(index = i, count = rows.size),
        leadingContent = {
          Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            SymbolIcon(icon, tint = MaterialTheme.colorScheme.onSecondaryContainer)
          }
        },
        trailingContent = { Text(value, style = MaterialTheme.typography.titleMedium) },
      ) { Text(label) }
    }
  }
}

@Composable
private fun SunToday(fasting: Boolean) {
  TodayShell { _ ->
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
      if (maxWidth >= 840.dp) {
        // Expanded: band becomes a rounded panel beside a details pane.
        Row(Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
          SunBand(fasting, Modifier.weight(1f).fillMaxHeight(), fill = true, shape = RoundedCornerShape(48.dp))
          Box(Modifier.weight(1f).fillMaxHeight()) {
            Column(Modifier.widthIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
              SunDetails(fasting)
              SunWeek()
              PrimaryAction(fasting)
              RecentFasts()
            }
          }
        }
      } else {
        // Compact and medium: one column (capped on foldables) under a full-width band.
        val medium = maxWidth >= 600.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
          SunBand(fasting, Modifier.fillMaxWidth())
          Column(Modifier.widthIn(max = 520.dp).padding(horizontal = if (medium) 24.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            SunDetails(fasting)
            PrimaryAction(fasting)
            SunWeek()
            if (medium) RecentFasts()
            Spacer(Modifier.height(8.dp))
          }
        }
      }
    }
  }
}

val preview =
  designBoard(name = "ONE — Today", seed = 0xFFB4653AL, variant = PaletteVariant.TonalSpot, brandFont = BundledFont.GoogleSansFlex) {
    direction("Ring, refined") {
      screen("fasting") { RingToday(fasting = true) }
      screen("idle") { RingToday(fasting = false) }
    }
    direction("Countdown numeral") {
      screen("fasting") { NumeralToday(fasting = true) }
      screen("idle") { NumeralToday(fasting = false) }
    }
    direction("Sunrise dial") {
      screen("fasting") { SunToday(fasting = true) }
      screen("idle") { SunToday(fasting = false) }
    }
  }
