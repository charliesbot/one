package com.charliesbot.one.features.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            SettingsGroup(
                title = "Debug",
                items = listOf(
                    {
                        ActionSettingItem(
                            label = "Test Snackbar",
                            description = "Trigger a test snackbar message",
                            onClick = { viewModel.testSnackbar() }
                        )
                    }
                )
            )

            Spacer(modifier = Modifier.height(32.dp))
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
