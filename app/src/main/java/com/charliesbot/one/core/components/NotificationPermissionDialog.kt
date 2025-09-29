package com.charliesbot.one.core.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.charliesbot.one.R
import com.charliesbot.one.ui.theme.OneTheme


@Composable
fun NotificationPermissionDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notification_permission_title)) },
        icon = {
            Icon(
                painter = painterResource(com.charliesbot.shared.R.drawable.notifications_24px),
                contentDescription = stringResource(R.string.notification_permission_icon_desc)
            )
        },
        text = {
            Text(stringResource(R.string.notification_permission_text))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.notification_permission_enable))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.notification_permission_not_now))
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
