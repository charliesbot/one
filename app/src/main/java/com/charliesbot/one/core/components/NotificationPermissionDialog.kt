package com.charliesbot.one.core.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.charliesbot.one.ui.theme.OneTheme


@Composable
fun NotificationPermissionDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enable Notifications") },
        icon = {
            Icon(
                Icons.Rounded.Notifications,
                contentDescription = "Notification Permission"
            )
        },
        text = {
            Text(
                "To keep track of your fasting progress, we'll need to send you notifications. " +
                        "\n\nWould you like to enable notifications?"
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Enable")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun NotificationPermissionDialogPreview() {
    OneTheme {
        NotificationPermissionDialog(onConfirm = {}, onDismiss = {})
    }
}
