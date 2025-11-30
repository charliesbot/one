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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
                .widthIn(max = 600.dp)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Notifications Section
            SettingsSection(title = stringResource(R.string.settings_notifications_title)) {
                SwitchSettingItem(
                    label = stringResource(R.string.settings_notifications_enabled),
                    description = stringResource(R.string.settings_notifications_enabled_desc),
                    checked = uiState.notificationsEnabled,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) },
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
                        )
                        SwitchSettingItem(
                            label = stringResource(R.string.settings_notify_one_hour),
                            description = stringResource(R.string.settings_notify_one_hour_desc),
                            checked = uiState.notifyOneHourBefore,
                            onCheckedChange = { viewModel.setNotifyOneHourBefore(it) },
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Data Management Section
            SettingsSection(title = stringResource(R.string.settings_data_title)) {
                ActionSettingItem(
                    label = stringResource(R.string.settings_export_history),
                    description = stringResource(R.string.settings_export_history_desc),
                    isLoading = uiState.isExporting,
                    onClick = { viewModel.exportHistory() }
                )
                SettingsDivider()
                ActionSettingItem(
                    label = stringResource(R.string.settings_force_sync),
                    description = stringResource(R.string.settings_force_sync_desc),
                    isLoading = uiState.isSyncing,
                    onClick = { viewModel.forceSyncToWatch() }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // App Info Section
            SettingsSection(title = stringResource(R.string.settings_app_info_title)) {
                SettingTile(
                    title = stringResource(R.string.settings_version),
                    trailingContent = {
                        Text(
                            text = versionName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                SettingsDivider()
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

            Spacer(modifier = Modifier.height(32.dp))
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
        modifier = modifier
            .widthIn(max = 600.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingTile(
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
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
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
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
        onClick = if (isLoading) null else onClick,
        trailingContent = {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                // Optional: You can add a chevron icon here if you want
                // Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
            }
        }
    )
}
