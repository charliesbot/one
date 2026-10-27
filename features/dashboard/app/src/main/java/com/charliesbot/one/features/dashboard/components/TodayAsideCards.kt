package com.charliesbot.one.features.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.charliesbot.one.features.dashboard.model.WeekDay
import com.charliesbot.shared.core.designsystem.common.R as DesignSystemR
import com.charliesbot.shared.core.strings.R

/** One choice in the inline goal picker. */
internal data class GoalOption(val id: String, val label: String)

/** The large-screen fast window card while fasting. */
internal data class FastWindowCardState(
  val progress: Float,
  val bedtimeFraction: Float?,
  /** "Thu, 5:03 AM". */
  val start: String,
  val end: String,
  /** "Asleep since 10 PM" or "Bedtime at 10 PM", when bedtime falls inside the fast. */
  val bedtimeNote: String?,
  /** What a screen reader says for the bar: progress and both ends. */
  val description: String,
)

/** Large screens: the week strip in its own card, with hours under each day. */
@Composable
internal fun WeekCard(days: List<WeekDay>, modifier: Modifier = Modifier) {
  // The strip gets more room than the other cards' content, for seven columns.
  AsideCard(stringResource(R.string.this_week), modifier, contentInset = 16.dp) {
    WeekStrip(days, showHours = true)
  }
}

/** Large screens, idle: the goals as chips instead of a bottom sheet. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GoalPickerCard(
  goals: List<GoalOption>,
  selectedGoalId: String,
  onSelectGoal: (String) -> Unit,
  onNewGoal: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AsideCard(stringResource(R.string.fasting_goal), modifier) {
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

/** Large screens, active: the fast window filling up, with bedtime marked. */
@Composable
internal fun FastWindowCard(state: FastWindowCardState, modifier: Modifier = Modifier) {
  AsideCard(stringResource(R.string.fast_window), modifier) {
    FastWindowProgressBar(
      progress = state.progress,
      bedtimeFraction = state.bedtimeFraction,
      start = state.start,
      end = state.end,
      description = state.description,
    )
    state.bedtimeNote?.let { note ->
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

/** A titled card on the surface beside the week, shared by the large-screen aside cards. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AsideCard(
  title: String,
  modifier: Modifier = Modifier,
  contentInset: Dp = 24.dp,
  content: @Composable ColumnScope.() -> Unit,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(28.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Column(Modifier.padding(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(
        title,
        style = MaterialTheme.typography.titleMediumEmphasized,
        modifier = Modifier.padding(horizontal = 24.dp),
      )
      Column(
        modifier = Modifier.padding(horizontal = contentInset),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
      )
    }
  }
}
