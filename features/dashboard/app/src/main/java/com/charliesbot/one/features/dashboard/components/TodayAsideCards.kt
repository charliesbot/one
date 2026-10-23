package com.charliesbot.one.features.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.charliesbot.one.features.dashboard.model.WeekDay
import com.charliesbot.shared.core.designsystem.common.R as DesignSystemR
import com.charliesbot.shared.core.strings.R

/** One choice in the inline goal picker. */
internal data class GoalOption(val id: String, val label: String)

private val AsideShape = RoundedCornerShape(28.dp)

/** Large screens: the week strip in its own card, with hours under each day. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun WeekCard(days: List<WeekDay>, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    shape = AsideShape,
    color = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
      Text(
        stringResource(R.string.this_week),
        style = MaterialTheme.typography.titleMediumEmphasized,
        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
      )
      WeekStrip(days, showHours = true)
    }
  }
}

/** Large screens, idle: the goals as chips instead of a bottom sheet. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun GoalPickerCard(
  goals: List<GoalOption>,
  selectedGoalId: String,
  onSelectGoal: (String) -> Unit,
  onNewGoal: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = AsideShape,
    color = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        stringResource(R.string.fasting_goal),
        style = MaterialTheme.typography.titleMediumEmphasized,
      )
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        goals.forEach { goal ->
          val selected = goal.id == selectedGoalId
          FilterChip(
            selected = selected,
            onClick = { onSelectGoal(goal.id) },
            label = { Text(goal.label) },
            leadingIcon =
              if (selected) {
                {
                  Icon(
                    painter = painterResource(DesignSystemR.drawable.check_24px),
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                  )
                }
              } else {
                null
              },
          )
        }
        AssistChip(
          onClick = onNewGoal,
          label = { Text(stringResource(R.string.new_goal)) },
          leadingIcon = {
            Icon(
              painter = painterResource(DesignSystemR.drawable.add_24px),
              contentDescription = null,
              modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
          },
        )
      }
    }
  }
}

/** Large screens, active: the fast window filling up, with bedtime marked. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun FastWindowCard(
  progress: Float,
  bedtimeFraction: Float?,
  start: String,
  end: String,
  bedtimeNote: String?,
  description: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = AsideShape,
    color = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        stringResource(R.string.fast_window),
        style = MaterialTheme.typography.titleMediumEmphasized,
      )
      FastWindowProgressBar(
        progress = progress,
        bedtimeFraction = bedtimeFraction,
        start = start,
        end = end,
        description = description,
      )
      bedtimeNote?.let { note ->
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            painter = painterResource(DesignSystemR.drawable.bedtime_filled_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
          Text(
            note,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
