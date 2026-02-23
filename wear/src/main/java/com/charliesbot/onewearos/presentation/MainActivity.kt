/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.charliesbot.onewearos.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.charliesbot.onewearos.core.components.NotificationPermissionDialog
import com.charliesbot.onewearos.presentation.navigation.WearNavigation
import com.charliesbot.onewearos.presentation.theme.OneTheme
import com.charliesbot.shared.core.notifications.NotificationUtil


class MainActivity : ComponentActivity() {
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                NotificationUtil.createNotificationChannel(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            var showNotificationPermission by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    showNotificationPermission = true
                }
            }

            OneTheme {
                WearNavigation()
                NotificationPermissionDialog(
                    isVisible = showNotificationPermission,
                    onDismiss = { showNotificationPermission = false },
                    onConfirm = {
                        showNotificationPermission = false
                        requestNotificationPermission.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    }
                )
            }
        }
    }
}

