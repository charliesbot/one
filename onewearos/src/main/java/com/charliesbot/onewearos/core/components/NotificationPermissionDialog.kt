package com.charliesbot.onewearos.core.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.charliesbot.onewearos.presentation.theme.OneTheme

@Composable
fun NotificationPermissionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isVisible: Boolean = true
) {
    AlertDialog(
        visible = isVisible,
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Rounded.Notifications,
                contentDescription = "Notification Permission"
            )
        },
        title = { Text("Enable Notifications") },
        text = {
            Text(
                "Allow notifications for fasting alerts?",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(
                onClick = onConfirm,
            )
        },
        dismissButton = {
            AlertDialogDefaults.DismissButton(
                onClick = onDismiss
            )
        }
    )
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
private fun NotificationPermissionDialogPreview() {
    OneTheme {
        NotificationPermissionDialog(
            onConfirm = {},
            onDismiss = {})
    }
}
