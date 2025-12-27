package com.charliesbot.one.features.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charliesbot.one.features.BuildConfig
import com.charliesbot.shared.R
import com.charliesbot.shared.core.data.repositories.settingsRepository.SmartReminderMode
import org.koin.androidx.compose.koinViewModel

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

    // Observe side effects
    LaunchedEffect(viewModel.sideEffects) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is SettingsSideEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(context.getString(effect.messageRes))
                }
            }
        }
    }

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
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                    )
                })
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Smart Reminders Section
            SettingsGroup(
                title = stringResource(R.string.settings_smart_reminders_title),
                items = listOf(
                    {
                        SwitchSettingItem(
                            label = stringResource(R.string.settings_smart_reminders_enabled),
                            description = stringResource(R.string.settings_smart_reminders_enabled_desc),
                            checked = uiState.smartRemindersEnabled,
                            onCheckedChange = { viewModel.setSmartRemindersEnabled(it) },
                        )
                    },
                    {
                        // Mode selector
                        SmartReminderModeSelector(
                            currentMode = uiState.smartReminderMode,
                            onModeSelected = { viewModel.setSmartReminderMode(it) }
                        )
                    },
                    {
                        val bedtimeHours = uiState.bedtimeMinutes / 60
                        val bedtimeMins = uiState.bedtimeMinutes % 60
                        val formattedBedtime = String.format("%02d:%02d", bedtimeHours % 24, bedtimeMins)

                        SettingTile(
                            title = stringResource(R.string.settings_bedtime),
                            description = stringResource(R.string.settings_bedtime_desc),
                            onClick = { showBedtimePicker = true },
                            trailingContent = {
                                Text(
                                    text = formattedBedtime,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    },
                    {
                        // Status info
                        SmartReminderStatusText(
                            suggestedTimeMinutes = suggestedFastingTime?.suggestedTimeMinutes,
                            reasoning = suggestedFastingTime?.reasoning
                        )
                    }
                )
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
                            onClick = { viewModel.exportHistory() }
                        )
                    },
                    {

                        ActionSettingItem(
                            label = stringResource(R.string.settings_force_sync),
                            description = stringResource(R.string.settings_force_sync_desc),
                            isLoading = uiState.isSyncing,
                            onClick = { viewModel.forceSyncToWatch() }
                        )
                    })
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App Info Section
            SettingsGroup(
                title = stringResource(R.string.settings_app_info_title),
                items = listOf(
                    {
                        SettingTile(
                            title = stringResource(R.string.settings_version),
                            onClick = { viewModel.copyVersionToClipboard() },
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
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data =
                                        android.net.Uri.parse("market://details?id=${context.packageName}")
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Debug Section
            if (BuildConfig.DEBUG) {
                SettingsGroup(
                    title = "Debug",
                    items = listOf(
                        {
                            ActionSettingItem(
                                label = "Test Snackbar",
                                description = "Trigger a test snackbar message",
                                onClick = { viewModel.testSnackbar() }
                            )
                        },
                        {
                            ActionSettingItem(
                                label = "Insert Mock Fasting Records",
                                description = "Add 5 mock records to test moving average",
                                onClick = { viewModel.insertMockFastingRecords() }
                            )
                        }
                    )
                )
            }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Bedtime Picker Dialog
    if (showBedtimePicker) {
        BedtimePickerDialog(
            currentBedtimeMinutes = uiState.bedtimeMinutes,
            onConfirm = { minutes ->
                viewModel.setBedtimeMinutes(minutes)
                showBedtimePicker = false
            },
            onDismiss = { showBedtimePicker = false }
        )
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
            // KEY 1: This creates the "cut" between items
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items.forEachIndexed { index, itemContent ->

                // KEY 2: Calculate shape based on position
                val shape = when {
                    items.size == 1 -> RoundedCornerShape(24.dp) // Single item
                    index == 0 -> RoundedCornerShape( // Top item
                        topStart = 24.dp, topEnd = 24.dp,
                        bottomStart = 4.dp, bottomEnd = 4.dp
                    )

                    index == items.lastIndex -> RoundedCornerShape( // Bottom item
                        topStart = 4.dp, topEnd = 4.dp,
                        bottomStart = 24.dp, bottomEnd = 24.dp
                    )

                    else -> RoundedCornerShape(4.dp) // Middle items
                }

                // KEY 3: Wrap each item in its own Surface
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BedtimePickerDialog(
    currentBedtimeMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val initialHour = currentBedtimeMinutes / 60
    val initialMinute = currentBedtimeMinutes % 60

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = stringResource(R.string.settings_bedtime),
                    style = MaterialTheme.typography.labelMedium
                )
                TimePicker(state = timePickerState)
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    androidx.compose.material3.TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.time_picker_cancel))
                    }
                    androidx.compose.material3.TextButton(onClick = {
                        val selectedMinutes = timePickerState.hour * 60 + timePickerState.minute
                        onConfirm(selectedMinutes)
                    }) {
                        Text(stringResource(R.string.time_picker_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartReminderModeSelector(
    currentMode: SmartReminderMode,
    onModeSelected: (SmartReminderMode) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Calculation Mode",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SmartReminderMode.entries.forEach { mode ->
            val label = when (mode) {
                SmartReminderMode.AUTO -> "Auto (recommended)"
                SmartReminderMode.BEDTIME_ONLY -> "Bedtime only"
                SmartReminderMode.MOVING_AVERAGE_ONLY -> "Moving average"
            }
            val description = when (mode) {
                SmartReminderMode.AUTO -> "Uses moving average if enough history, otherwise bedtime"
                SmartReminderMode.BEDTIME_ONLY -> "Always calculate from your bedtime setting"
                SmartReminderMode.MOVING_AVERAGE_ONLY -> "Use your recent fasting start times"
            }

            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onModeSelected(mode) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.RadioButton(
                    selected = currentMode == mode,
                    onClick = { onModeSelected(mode) }
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartReminderStatusText(
    suggestedTimeMinutes: Int?,
    reasoning: String?
) {
    val statusText = when {
        suggestedTimeMinutes != null && reasoning != null -> {
            val hours = suggestedTimeMinutes / 60
            val mins = suggestedTimeMinutes % 60
            val formattedTime = String.format("%02d:%02d", hours % 24, mins)
            "Suggested start: $formattedTime\n$reasoning"
        }
        else -> "Not enough data yet. Using bedtime fallback."
    }

    Text(
        text = statusText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
