package com.charliesbot.one

import android.os.Build
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.charliesbot.one.core.components.NotificationPermissionDialog
import com.charliesbot.shared.core.notifications.NotificationUtil
import com.charliesbot.one.today.TodayScreen
import com.charliesbot.one.ui.theme.OneTheme
import com.charliesbot.one.widgets.updateWidgetPreview
import org.koin.androidx.compose.KoinAndroidContext

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                NotificationUtil.createNotificationChannel(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        updateWidgetPreview(this)
        setContent {
            var showNotificationPermission by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    NotificationUtil.createNotificationChannel(this@MainActivity)
                    return@LaunchedEffect
                }

                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    showNotificationPermission = true
                }

            }

            OneTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        TodayScreen()

                        if (showNotificationPermission) {
                            NotificationPermissionDialog(
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
        }
    }
}