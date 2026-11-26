@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bot.charlies.androiddesign.preview.kit.*

// Baseline: the ONE phone UI as shipped (Nov 2026). Faithful to current screens, not a proposal.
// Source: features/{dashboard,profile,settings}/app, app/ui/theme, core/designsystem.
// Seed is the static fallback scheme's primary (Purple40); the shipped app uses dynamic color.

private enum class Tab { Today, You, Settings }

// TopAppBar titles use titleLarge = GoogleSansTitleLargeSpecific (wght 761, wdth 130.5, GRAD -108).
private val appBarTitle =
  TextStyle(
    fontFamily = bundledFontFamily(BundledFont.GoogleSansFlex, weight = 761, variation = "'wdth' 130.5, 'GRAD' -108"),
    fontWeight = FontWeight(761),
    fontSize = 28.sp,
    lineHeight = 28.sp,
  )

// headlineMedium = GoogleSansWide (wdth 125) at weight 621.
private val headlineWide =
  TextStyle(
    fontFamily = bundledFontFamily(BundledFont.GoogleSansFlex, weight = 621, variation = "'wdth' 125"),
    fontWeight = FontWeight(621),
    fontSize = 28.sp,
    lineHeight = 36.sp,
  )

@Composable
private fun AppShell(selected: Tab, title: String, content: @Composable (Modifier) -> Unit) {
  Scaffold(
    topBar = { TopAppBar(title = { Text(title, style = appBarTitle) }) },
    bottomBar = {
      NavigationBar {
        listOf(Tab.Today to "today", Tab.You to "person", Tab.Settings to "settings").forEach { (tab, icon) ->
          NavigationBarItem(
            selected = selected == tab,
            onClick = {},
            icon = { SymbolIcon(icon, contentDescription = tab.name) },
            label = { Text(tab.name) },
          )
        }
      }
    },
  ) { padding -> content(Modifier.padding(padding)) }
}

// Mirrors core/designsystem FastingProgressBar: butt-capped arc over a surfaceVariant track.
@Composable
private fun FastingProgressBar(progress: Float, strokeWidth: Dp, modifier: Modifier = Modifier) {
  val indicator = MaterialTheme.colorScheme.primary
  val track = MaterialTheme.colorScheme.surfaceVariant
  Canvas(modifier = modifier.aspectRatio(1f)) {
    val stroke = strokeWidth.toPx()
    val radius = size.minDimension / 2 - stroke / 2
    drawCircle(color = track, radius = radius, style = Stroke(width = stroke))
    drawArc(
      color = indicator,
      startAngle = -90f,
      sweepAngle = progress * 360f,
      useCenter = false,
      topLeft = Offset(center.x - radius, center.y - radius),
      size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
      style = Stroke(width = stroke, cap = StrokeCap.Butt),
    )
  }
}

@Composable
private fun WeeklyProgress(modifier: Modifier = Modifier) {
  val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
  val progress = listOf(1f, 1f, 0.85f, 0.83f, 0f, 0f, 0f)
  Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = modifier) {
    days.forEachIndexed { i, day ->
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(day, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.size(4.dp))
        FastingProgressBar(progress[i], strokeWidth = 5.dp, modifier = Modifier.size(25.dp))
      }
    }
  }
}

@Composable
private fun CurrentFastingProgress(isFasting: Boolean) {
  Box(contentAlignment = Alignment.Center) {
    FastingProgressBar(
      progress = if (isFasting) 0.83f else 0f,
      strokeWidth = 35.dp,
      modifier = Modifier.fillMaxWidth().aspectRatio(1f, matchHeightConstraintsFirst = true).heightIn(max = 300.dp),
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(if (isFasting) "Elapsed (83%)" else "UPCOMING FAST", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
      TextButton(onClick = {}) {
        Text(
          if (isFasting) "13:20:45" else "16 hours",
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 38.sp,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
private fun TimeDisplay(title: String, value: String, leading: Boolean, modifier: Modifier = Modifier) {
  Column(modifier = modifier) {
    Text(
      title.uppercase(),
      modifier = Modifier.fillMaxWidth(),
      textAlign = TextAlign.Center,
      fontSize = 10.sp,
      fontWeight = FontWeight.W500,
      color = MaterialTheme.colorScheme.onSurface,
    )
    OutlinedToggleButton(
      checked = false,
      onCheckedChange = {},
      shapes =
        if (leading) ButtonGroupDefaults.connectedLeadingButtonShapes(pressedShape = RoundedCornerShape(60.dp))
        else ButtonGroupDefaults.connectedTrailingButtonShapes(pressedShape = RoundedCornerShape(60.dp)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
  }
}

@Composable
private fun FastControls(isFasting: Boolean) {
  if (isFasting) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
      TimeDisplay("Started", "Wed, 8:00 PM", leading = true, modifier = Modifier.weight(1f))
      TimeDisplay("16H Goal", "Thu, 12:00 PM", leading = false, modifier = Modifier.weight(1f))
    }
  }
  Spacer(Modifier.height(10.dp))
  FilledTonalButton(
    onClick = {},
    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("ad:primary"),
  ) { Text(if (isFasting) "End Fast" else "Start Fasting") }
}

@Composable
private fun SmartSuggestionCard(modifier: Modifier = Modifier) {
  val on = MaterialTheme.colorScheme.onSecondaryContainer
  Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
    Column(Modifier.padding(16.dp)) {
      Text("Upcoming fast", style = MaterialTheme.typography.labelMedium, color = on)
      Spacer(Modifier.height(4.dp))
      Text("8:00 PM", style = headlineWide, color = on)
      Spacer(Modifier.height(4.dp))
      // Shipped: onSecondaryContainer at 70% alpha.
      Text("Based on your recent 7-day average", style = MaterialTheme.typography.bodySmall, color = on)
      Spacer(Modifier.height(4.dp))
      Text("Based on recent average", style = MaterialTheme.typography.labelSmall, color = on)
    }
  }
}

@Composable
private fun TodayScreen(isFasting: Boolean) {
  AppShell(Tab.Today, "Today") { mod ->
    BoxWithConstraints(mod.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
      val medium = maxWidth >= 600.dp
      val pad = 32.dp
      Column(
        Modifier.widthIn(max = if (medium) 800.dp else 600.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        WeeklyProgress(Modifier.fillMaxWidth().padding(horizontal = pad + 24.dp))
        if (!isFasting) {
          SmartSuggestionCard(Modifier.fillMaxWidth().padding(start = pad, end = pad, top = 30.dp))
        }
        Card(
          modifier = Modifier.fillMaxWidth().padding(start = pad, end = pad, top = 30.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
          if (medium) {
            Row(
              Modifier.padding(pad).fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(32.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(Modifier.weight(1f)) { CurrentFastingProgress(isFasting) }
              Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { FastControls(isFasting) }
            }
          } else {
            Column(Modifier.padding(pad), horizontalAlignment = Alignment.CenterHorizontally) {
              CurrentFastingProgress(isFasting)
              Spacer(Modifier.height(40.dp))
              FastControls(isFasting)
            }
          }
        }
      }
    }
  }
}

// November 2026 starts on Sunday; calendar uses Sunday-first weeks. Goal is 16h.
private val monthHours: Map<Int, Int> =
  (1..25).filter { it % 3 != 0 }.associateWith { listOf(16, 17, 14, 18, 16, 13, 20, 16)[it % 8] }

@Composable
private fun DayCell(day: Int, modifier: Modifier = Modifier) {
  val hours = monthHours[day]
  val met = hours != null && hours >= 16
  val bg = when {
    met -> MaterialTheme.colorScheme.primary
    hours != null -> MaterialTheme.colorScheme.surfaceVariant
    else -> null
  }
  val fg = when {
    met -> MaterialTheme.colorScheme.onPrimary
    hours != null -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.onSurface
  }
  Box(modifier, contentAlignment = Alignment.Center) {
    Box(
      Modifier.padding(4.dp).size(44.dp).clip(MaterialShapes.Square.toShape()).then(if (bg != null) Modifier.background(bg) else Modifier).clickable {},
      contentAlignment = Alignment.Center,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$day", style = MaterialTheme.typography.bodyMedium, color = fg)
        if (hours != null) {
          Text(
            "${hours}h",
            style = MaterialTheme.typography.labelMediumEmphasized,
            color = fg,
            fontWeight = if (met) FontWeight.Bold else FontWeight.Normal,
          )
        }
      }
    }
  }
}

@Composable
private fun YouScreen() {
  AppShell(Tab.You, "You") { mod ->
    Column(mod.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
      Card(Modifier.padding(vertical = 12.dp, horizontal = 20.dp).widthIn(max = 600.dp)) {
        Column(Modifier.padding(16.dp)) {
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Nov '26", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              listOf("chevron_left" to "Previous month", "chevron_right" to "Next month").forEach { (icon, cd) ->
                FilledIconButton(
                  onClick = {},
                  shapes = IconButtonDefaults.shapes(),
                  colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                ) { SymbolIcon(icon, contentDescription = cd) }
              }
            }
          }
          Spacer(Modifier.height(28.dp))
          Row(Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
              Text(
                it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
              )
            }
          }
          Spacer(Modifier.height(8.dp))
          repeat(5) { week ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
              repeat(7) { dow ->
                val day = week * 7 + dow + 1
                if (day <= 30) DayCell(day, Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
              }
            }
          }
        }
      }
    }
  }
}

// Settings: grouped surfaceContainer tiles, 24dp outer / 4dp inner corners, 2dp gaps.
private fun groupShape(index: Int, count: Int) = when {
  count == 1 -> RoundedCornerShape(24.dp)
  index == 0 -> RoundedCornerShape(24.dp, 24.dp, 4.dp, 4.dp)
  index == count - 1 -> RoundedCornerShape(4.dp, 4.dp, 24.dp, 24.dp)
  else -> RoundedCornerShape(4.dp)
}

@Composable
private fun GroupTitle(text: String) {
  Text(
    text,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
  )
}

@Composable
private fun CheckSwitch(checked: Boolean) {
  Switch(
    checked = checked,
    onCheckedChange = {},
    thumbContent = { SymbolIcon(if (checked) "check" else "close", size = SwitchDefaults.IconSize) },
  )
}

@Composable
private fun SettingsGroup(title: String, rows: List<Triple<String, String?, (@Composable () -> Unit)?>>) {
  Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
    GroupTitle(title)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      rows.forEachIndexed { i, (label, desc, trailing) ->
        Surface(shape = groupShape(i, rows.size), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
          ListItem(
            modifier = Modifier.clickable {},
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            supportingContent = desc?.let { { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            trailingContent = trailing,
          ) { Text(label, style = MaterialTheme.typography.titleMedium) }
        }
      }
    }
  }
}

@Composable
private fun SmartRemindersCard() {
  Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
    GroupTitle("Smart Reminders")
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
      Column(Modifier.padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text("Enable Smart Reminders", style = MaterialTheme.typography.titleMedium)
            Text(
              "Get reminded when to start your fast based on your habits",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          CheckSwitch(true)
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
          Text("🕐", style = headlineWide)
          Spacer(Modifier.size(12.dp))
          Column {
            Text("Start your fast at 8:00 PM", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Based on your recent 7-day average", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().clickable {}.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
          Text("⚙️", style = MaterialTheme.typography.bodyLarge)
          Spacer(Modifier.size(8.dp))
          Text("Customize", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
          SymbolIcon("keyboard_arrow_up", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // Expanded state shown so the segmented mode picker is part of the baseline.
        Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
          Text(
            "Calculation Mode",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
          )
          val modes = listOf("Auto", "Bedtime", "History", "Fixed")
          SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            modes.forEachIndexed { i, m ->
              SegmentedButton(selected = i == 0, onClick = {}, shape = SegmentedButtonDefaults.itemShape(i, modes.size)) {
                Text(m, style = MaterialTheme.typography.labelSmall)
              }
            }
          }
          Spacer(Modifier.height(16.dp))
          Row(Modifier.fillMaxWidth().clickable {}.padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Target Bedtime", style = MaterialTheme.typography.bodyMedium)
            Text("10:00 PM", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun SettingsScreen() {
  AppShell(Tab.Settings, "Settings") { mod ->
    Box(mod.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
      Column(Modifier.widthIn(max = 600.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        SettingsGroup(
          "Notifications",
          listOf(Triple("Enable Notifications", "Receive fasting progress notifications") { CheckSwitch(true) }),
        )
        Spacer(Modifier.height(16.dp))
        SmartRemindersCard()
        Spacer(Modifier.height(16.dp))
        SettingsGroup(
          "Data Management",
          listOf(
            Triple("Export History (CSV)", "Export your fasting history as a CSV file", null),
            Triple("Force Sync to Watch", "Manually sync current state to your watch", null),
          ),
        )
        Spacer(Modifier.height(16.dp))
        SettingsGroup(
          "App Info",
          listOf(
            Triple("Version", null) {
              Text("1.42-mobile", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            Triple("Rate App", "Help us by rating on the Play Store", null),
          ),
        )
        Spacer(Modifier.height(32.dp))
      }
    }
  }
}

val preview =
  designBoard(name = "ONE — Baseline", seed = 0xFF6650A4L, variant = PaletteVariant.TonalSpot, brandFont = BundledFont.GoogleSansFlex) {
    direction("Shipped") {
      screen("today-fasting") { TodayScreen(isFasting = true) }
      screen("today-idle") { TodayScreen(isFasting = false) }
      screen("you") { YouScreen() }
      screen("settings") { SettingsScreen() }
    }
  }
