package com.charliesbot.one.features.settings

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import com.charliesbot.one.features.dashboard.components.TimePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charliesbot.shared.R
import com.charliesbot.shared.core.data.repositories.settingsRepository.SmartReminderMode
import com.charliesbot.shared.core.models.SuggestedFastingTime
import com.charliesbot.shared.core.utils.formatMinutesAsTime
import androidx.compose.ui.tooling.preview.Preview
import com.charliesbot.shared.core.models.SuggestionSource
import org.koin.androidx.compose.koinViewModel
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val suggestedFastingTime by viewModel.suggestedFastingTime.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showBedtimePicker by remember { mutableStateOf(false) }
    var showFixedTimePicker by remember { mutableStateOf(false) }

    // Observe side effects
    LaunchedEffect(viewModel.sideEffects) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is SettingsSideEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    SettingsScreenContent(
        uiState = uiState,
        suggestedFastingTime = suggestedFastingTime,
        snackbarHostState = snackbarHostState,
        onNotificationsEnabledChange = viewModel::setNotificationsEnabled,
        onSmartRemindersEnabledChange = viewModel::setSmartRemindersEnabled,
        onSmartReminderModeSelected = viewModel::setSmartReminderMode,
        onBedtimeClick = { showBedtimePicker = true },
        onFixedTimeClick = { showFixedTimePicker = true },
        onExportHistory = viewModel::exportHistory,
        onForceSyncToWatch = viewModel::forceSyncToWatch,
        onCopyVersionToClipboard = viewModel::copyVersionToClipboard,
        onRateAppClick = {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = "market://details?id=${context.packageName}".toUri()
            }
            context.startActivity(intent)
        }
    )

    // Bedtime Picker Dialog
    if (showBedtimePicker) {
        TimePickerDialog(
            title = stringResource(R.string.settings_bedtime),
            currentMinutes = uiState.bedtimeMinutes,
            onConfirm = { minutes ->
                viewModel.setBedtimeMinutes(minutes)
                showBedtimePicker = false
            },
            onDismiss = { showBedtimePicker = false }
        )
    }

    // Fixed Time Picker Dialog
    if (showFixedTimePicker) {
        TimePickerDialog(
            title = stringResource(R.string.settings_fasting_start_time),
            currentMinutes = uiState.fixedFastingStartMinutes,
            onConfirm = { minutes ->
                viewModel.setFixedFastingStartMinutes(minutes)
                showFixedTimePicker = false
            },
            onDismiss = { showFixedTimePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContent(
    uiState: SettingsUiState,
    suggestedFastingTime: SuggestedFastingTime?,
    snackbarHostState: SnackbarHostState,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onSmartRemindersEnabledChange: (Boolean) -> Unit,
    onSmartReminderModeSelected: (SmartReminderMode) -> Unit,
    onBedtimeClick: () -> Unit,
    onFixedTimeClick: () -> Unit,
    onExportHistory: () -> Unit,
    onForceSyncToWatch: () -> Unit,
    onCopyVersionToClipboard: () -> Unit,
    onRateAppClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                windowInsets = WindowInsets(top = 0.dp)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Notifications Section
                SettingsGroup(
                    title = stringResource(R.string.settings_notifications_title),
                    items = listOf({
                        SwitchSettingItem(
                            label = stringResource(R.string.settings_notifications_enabled),
                            description = stringResource(R.string.settings_notifications_enabled_desc),
                            checked = uiState.notificationsEnabled,
                            onCheckedChange = onNotificationsEnabledChange,
                        )
                    })
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Smart Reminders Section - New Design
                SmartRemindersCard(
                    enabled = uiState.smartRemindersEnabled,
                    onEnabledChange = onSmartRemindersEnabledChange,
                    suggestedFastingTime = suggestedFastingTime,
                    currentMode = uiState.smartReminderMode,
                    onModeSelected = onSmartReminderModeSelected,
                    bedtimeMinutes = uiState.bedtimeMinutes,
                    onBedtimeClick = onBedtimeClick,
                    fixedStartMinutes = uiState.fixedFastingStartMinutes,
                    onFixedTimeClick = onFixedTimeClick
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Data Management Section
                SettingsGroup(
                    title = stringResource(R.string.settings_data_title),
                    items = listOf(
                        {
                            ActionSettingItem(
                                label = stringResource(R.string.settings_export_history),
                                description = stringResource(R.string.settings_export_history_desc),
                                isLoading = uiState.isExporting,
                                onClick = onExportHistory
                            )
                        },
                        {
                            ActionSettingItem(
                                label = stringResource(R.string.settings_force_sync),
                                description = stringResource(R.string.settings_force_sync_desc),
                                isLoading = uiState.isSyncing,
                                onClick = onForceSyncToWatch
                            )
                        }
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // App Info Section
                SettingsGroup(
                    title = stringResource(R.string.settings_app_info_title),
                    items = listOf(
                        {
                            SettingTile(
                                title = stringResource(R.string.settings_version),
                                onClick = onCopyVersionToClipboard,
                                trailingContent = {
                                    Text(
                                        text = uiState.versionName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        },
                        {
                            ActionSettingItem(
                                label = stringResource(R.string.settings_rate_app),
                                description = stringResource(R.string.settings_rate_app_desc),
                                onClick = onRateAppClick
                            )
                        }
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmartRemindersCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    suggestedFastingTime: SuggestedFastingTime?,
    currentMode: SmartReminderMode,
    onModeSelected: (SmartReminderMode) -> Unit,
    bedtimeMinutes: Int,
    onBedtimeClick: () -> Unit,
    fixedStartMinutes: Int,
    onFixedTimeClick: () -> Unit
) {
    var isCustomizeExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Section Title
        Text(
            text = stringResource(R.string.settings_smart_reminders_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header with toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = stringResource(R.string.settings_smart_reminders_enabled),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.settings_smart_reminders_enabled_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = onEnabledChange,
                        thumbContent = {
                            Icon(
                                painterResource(if (enabled) R.drawable.check_24px else R.drawable.close_24px),
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    )
                }

                // Suggested time display (only show when enabled)
                if (enabled && suggestedFastingTime != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Prominent time display
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "🕐",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Column {
                            val formattedTime = formatMinutesAsTime(suggestedFastingTime.suggestedTimeMinutes)

                            Text(
                                text = stringResource(R.string.settings_start_fast_at, formattedTime),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = suggestedFastingTime.reasoning,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Customize expandable section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCustomizeExpanded = !isCustomizeExpanded }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⚙️", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = stringResource(R.string.settings_customize),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            painter = painterResource(
                                if (isCustomizeExpanded) R.drawable.keyboard_arrow_up_24px
                                else R.drawable.keyboard_arrow_down_24px
                            ),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Expandable settings
                    AnimatedVisibility(
                        visible = isCustomizeExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            // Mode selector with segmented buttons
                            Text(
                                text = stringResource(R.string.settings_calculation_mode),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            val modeAutoLabel = stringResource(R.string.settings_mode_auto)
                            val modeBedtimeLabel = stringResource(R.string.settings_mode_bedtime)
                            val modeHistoryLabel = stringResource(R.string.settings_mode_history)
                            val modeFixedLabel = stringResource(R.string.settings_mode_fixed)

                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val modes = listOf(
                                    SmartReminderMode.AUTO to modeAutoLabel,
                                    SmartReminderMode.BEDTIME_ONLY to modeBedtimeLabel,
                                    SmartReminderMode.MOVING_AVERAGE_ONLY to modeHistoryLabel,
                                    SmartReminderMode.FIXED_TIME to modeFixedLabel
                                )
                                modes.forEachIndexed { index, (mode, label) ->
                                    SegmentedButton(
                                        selected = currentMode == mode,
                                        onClick = { onModeSelected(mode) },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = modes.size
                                        )
                                    ) {
                                        Text(label, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Time setting based on mode
                            when (currentMode) {
                                SmartReminderMode.FIXED_TIME -> {
                                    val formattedFixed = formatMinutesAsTime(fixedStartMinutes)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onFixedTimeClick() }
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.settings_fasting_start_time),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = formattedFixed,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                SmartReminderMode.BEDTIME_ONLY, SmartReminderMode.AUTO -> {
                                    val formattedBedtime = formatMinutesAsTime(bedtimeMinutes)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onBedtimeClick() }
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.settings_bedtime),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = formattedBedtime,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                SmartReminderMode.MOVING_AVERAGE_ONLY -> {
                                    Text(
                                        text = stringResource(R.string.settings_using_history),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingTile(
    modifier: Modifier = Modifier,
    title: String,
    description: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)
) {
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .then(Modifier.clickable(onClick = onClick)),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
        },
        supportingContent = if (description != null) {
            {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else null,
        trailingContent = trailingContent
    )
}

@Composable
private fun SwitchSettingItem(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingTile(
        title = label,
        description = description,
        modifier = modifier,
        onClick = { onCheckedChange(!checked) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                thumbContent = {
                    Icon(
                        painterResource(if (checked) R.drawable.check_24px else R.drawable.close_24px),
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            )
        }
    )
}

@Composable
private fun ActionSettingItem(
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    SettingTile(
        title = label,
        description = description,
        modifier = modifier,
        onClick = if (isLoading) ({}) else onClick,
        trailingContent = {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                null
            }
        }
    )
}

@Composable
fun SettingsGroup(
    title: String,
    items: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        // The Container
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items.forEachIndexed { index, itemContent ->
                val shape = when {
                    items.size == 1 -> RoundedCornerShape(24.dp)
                    index == 0 -> RoundedCornerShape(
                        topStart = 24.dp, topEnd = 24.dp,
                        bottomStart = 4.dp, bottomEnd = 4.dp
                    )
                    index == items.lastIndex -> RoundedCornerShape(
                        topStart = 4.dp, topEnd = 4.dp,
                        bottomStart = 24.dp, bottomEnd = 24.dp
                    )
                    else -> RoundedCornerShape(4.dp)
                }

                Surface(
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemContent()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSettingsScreen() {
    SettingsScreenContent(
        uiState = SettingsUiState(
            notificationsEnabled = true,
            smartRemindersEnabled = true,
            smartReminderMode = SmartReminderMode.AUTO,
            bedtimeMinutes = 1320, // 10:00 PM
            fixedFastingStartMinutes = 1140, // 7:00 PM
            versionName = "1.0.0"
        ),
        suggestedFastingTime = SuggestedFastingTime(
            suggestedTimeMillis = System.currentTimeMillis(),
            suggestedTimeMinutes = 1200, // 8:00 PM
            reasoning = "Based on your recent 7-day average",
            source = SuggestionSource.MOVING_AVERAGE
        ),
        snackbarHostState = remember { SnackbarHostState() },
        onNotificationsEnabledChange = {},
        onSmartRemindersEnabledChange = {},
        onSmartReminderModeSelected = {},
        onBedtimeClick = {},
        onFixedTimeClick = {},
        onExportHistory = {},
        onForceSyncToWatch = {},
        onCopyVersionToClipboard = {},
        onRateAppClick = {}
    )
}
