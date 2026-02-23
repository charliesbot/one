package com.charliesbot.onewearos.core.components

import com.charliesbot.onewearos.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
                painter = painterResource(com.charliesbot.shared.R.drawable.notifications_24px),
                contentDescription = "Notification Permission"
            )
        },
        title = { Text(stringResource(R.string.dialog_title_enable_notifications)) },
        text = {
            Text(
                text = stringResource(R.string.dialog_message_enable_notifications),
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
