package com.charliesbot.one.features.settings

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.charliesbot.shared.core.designsystem.common.time.ClockFormatProvider
import com.charliesbot.shared.core.designsystem.common.time.LocalClockFormat
import com.charliesbot.shared.core.models.TimeFormatMode
import com.charliesbot.shared.core.strings.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeFormatSetting(
  selected: TimeFormatMode,
  onSelected: (TimeFormatMode) -> Unit,
  modifier: Modifier = Modifier,
) {
  val options =
    listOf(
      TimeFormatMode.SYSTEM to stringResource(R.string.settings_time_format_system),
      TimeFormatMode.TWELVE_HOUR to stringResource(R.string.settings_time_format_12_hour),
      TimeFormatMode.TWENTY_FOUR_HOUR to stringResource(R.string.settings_time_format_24_hour),
    )
  val example = LocalClockFormat.current.minutes(18 * 60 + 30)

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
  MaterialTheme { TimeFormatSetting(selected = TimeFormatMode.SYSTEM, onSelected = {}) }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun TimeFormat12HourPreview() {
  ClockFormatProvider(false) {
    MaterialTheme { TimeFormatSetting(selected = TimeFormatMode.TWELVE_HOUR, onSelected = {}) }
  }
}

@Preview(showBackground = true, widthDp = 320, locale = "es")
@Composable
private fun TimeFormat24HourSpanishPreview() {
  ClockFormatProvider(true) {
    MaterialTheme { TimeFormatSetting(selected = TimeFormatMode.TWENTY_FOUR_HOUR, onSelected = {}) }
  }
}
