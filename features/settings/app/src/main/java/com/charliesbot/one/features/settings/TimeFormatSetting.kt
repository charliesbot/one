package com.charliesbot.one.features.settings

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.charliesbot.shared.core.strings.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter

internal enum class TimeFormatOption {
  SYSTEM,
  TWELVE_HOUR,
  TWENTY_FOUR_HOUR,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeFormatSetting(
  selected: TimeFormatOption,
  onSelected: (TimeFormatOption) -> Unit,
  modifier: Modifier = Modifier,
) {
  val options =
    listOf(
      TimeFormatOption.SYSTEM to stringResource(R.string.settings_time_format_system),
      TimeFormatOption.TWELVE_HOUR to stringResource(R.string.settings_time_format_12_hour),
      TimeFormatOption.TWENTY_FOUR_HOUR to stringResource(R.string.settings_time_format_24_hour),
    )
  val is24Hour =
    when (selected) {
      TimeFormatOption.SYSTEM -> DateFormat.is24HourFormat(LocalContext.current)
      TimeFormatOption.TWELVE_HOUR -> false
      TimeFormatOption.TWENTY_FOUR_HOUR -> true
    }
  val locale = LocalConfiguration.current.locales[0]
  val pattern = DateFormat.getBestDateTimePattern(locale, if (is24Hour) "Hm" else "hm")
  val example = LocalTime.of(18, 30).format(DateTimeFormatter.ofPattern(pattern, locale))

  Column(
    modifier = modifier.fillMaxWidth().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = stringResource(R.string.settings_time_format),
      style = MaterialTheme.typography.titleMedium,
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
      options.forEachIndexed { index, (option, label) ->
        SegmentedButton(
          selected = selected == option,
          onClick = { onSelected(option) },
          shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
        ) {
          Text(label, style = MaterialTheme.typography.labelLarge)
        }
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.settings_time_format_example),
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(
        text = example,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun TimeFormatSystemPreview() {
  MaterialTheme { TimeFormatSetting(selected = TimeFormatOption.SYSTEM, onSelected = {}) }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun TimeFormat12HourPreview() {
  MaterialTheme { TimeFormatSetting(selected = TimeFormatOption.TWELVE_HOUR, onSelected = {}) }
}

@Preview(showBackground = true, widthDp = 320, locale = "es")
@Composable
private fun TimeFormat24HourSpanishPreview() {
  MaterialTheme { TimeFormatSetting(selected = TimeFormatOption.TWENTY_FOUR_HOUR, onSelected = {}) }
}
