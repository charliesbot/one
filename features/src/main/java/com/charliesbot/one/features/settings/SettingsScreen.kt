package com.charliesbot.one.features.settings

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charliesbot.shared.R
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Get version from package manager
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    // Show snackbar messages
    LaunchedEffect(uiState.showExportSuccess) {
        if (uiState.showExportSuccess) {
            snackbarHostState.showSnackbar("History exported to Downloads folder")
            viewModel.dismissExportSuccess()
        }
    }

    LaunchedEffect(uiState.showExportError) {
        if (uiState.showExportError) {
            snackbarHostState.showSnackbar("Export failed. Check if you have records.")
            viewModel.dismissExportError()
        }
    }

    LaunchedEffect(uiState.showSyncSuccess) {
        if (uiState.showSyncSuccess) {
            snackbarHostState.showSnackbar("Sync to watch successful!")
            viewModel.dismissSyncSuccess()
        }
    }

    LaunchedEffect(uiState.showSyncError) {
        if (uiState.showSyncError) {
            snackbarHostState.showSnackbar("Sync failed. Check watch connection.")
            viewModel.dismissSyncError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Notifications Section
            SettingsSection(title = stringResource(R.string.settings_notifications_title)) {
                SwitchSettingItem(
                    label = stringResource(R.string.settings_notifications_enabled),
                    description = stringResource(R.string.settings_notifications_enabled_desc),
                    checked = uiState.notificationsEnabled,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                    enableClickableRow = true
                )

                AnimatedVisibility(
                    visible = uiState.notificationsEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        SwitchSettingItem(
                            label = stringResource(R.string.settings_notify_completion),
                            description = stringResource(R.string.settings_notify_completion_desc),
                            checked = uiState.notifyOnCompletion,
                            onCheckedChange = { viewModel.setNotifyOnCompletion(it) },
                            modifier = Modifier.padding(start = 16.dp),
                            enableClickableRow = true
                        )

                        SwitchSettingItem(
                            label = stringResource(R.string.settings_notify_one_hour),
                            description = stringResource(R.string.settings_notify_one_hour_desc),
                            checked = uiState.notifyOneHourBefore,
                            onCheckedChange = { viewModel.setNotifyOneHourBefore(it) },
                            modifier = Modifier.padding(start = 16.dp),
                            enableClickableRow = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Data Management Section
            SettingsSection(title = stringResource(R.string.settings_data_title)) {
                ButtonSettingItem(
                    label = stringResource(R.string.settings_export_history),
                    description = stringResource(R.string.settings_export_history_desc),
                    isLoading = uiState.isExporting,
                    onClick = { viewModel.exportHistory() }
                )

                ButtonSettingItem(
                    label = stringResource(R.string.settings_force_sync),
                    description = stringResource(R.string.settings_force_sync_desc),
                    isLoading = uiState.isSyncing,
                    onClick = { viewModel.forceSyncToWatch() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Info Section
            SettingsSection(title = stringResource(R.string.settings_app_info_title)) {
                TextSettingItem(
                    label = stringResource(R.string.settings_version),
                    value = versionName
                )

                ButtonSettingItem(
                    label = stringResource(R.string.settings_rate_app),
                    description = stringResource(R.string.settings_rate_app_desc),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse("market://details?id=${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.widthIn(max = 600.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SwitchSettingItem(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enableClickableRow: Boolean = false
) {
    val rowModifier = if (enableClickableRow) {
        modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp)
    } else {
        modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    }
    
    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ButtonSettingItem(
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Button(
            onClick = onClick,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Text(label)
        }
    }
}

@Composable
private fun TextSettingItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

