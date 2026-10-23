package com.charliesbot.one

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.charliesbot.one.core.components.NotificationPermissionDialog
import com.charliesbot.one.navigation.MainNavigation
import com.charliesbot.one.ui.theme.OneTheme
import com.charliesbot.one.widget.common.WidgetRefreshScheduler
import com.charliesbot.one.widget.updateWidgetPreview
import com.charliesbot.shared.core.data.notifications.NotificationUtil
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class MainActivity : ComponentActivity() {
  private val phoneWidgetRefreshScheduler: WidgetRefreshScheduler by inject()
  private var isTodayReady = false

  private val requestNotificationPermission =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
      if (isGranted) {
        NotificationUtil.createNotificationChannel(this)
      }
    }

  override fun onStart() {
    super.onStart()
    lifecycleScope.launch { phoneWidgetRefreshScheduler.reconcile() }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    updateWidgetPreview(this)
    keepSplashScreenUntilTodayIsReady()
    setContent {
      var showNotificationPermission by remember { mutableStateOf(false) }
      var selectedItem by remember { mutableIntStateOf(0) }

      LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
          NotificationUtil.createNotificationChannel(this@MainActivity)
          return@LaunchedEffect
        }

        if (
          ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.POST_NOTIFICATIONS,
          ) != PackageManager.PERMISSION_GRANTED
        ) {
          showNotificationPermission = true
        }
      }

      OneTheme {
        Box(modifier = Modifier.fillMaxSize()) {
          MainNavigation(onTodayReady = { isTodayReady = true })

          if (showNotificationPermission) {
            NotificationPermissionDialog(
              onDismiss = { showNotificationPermission = false },
              onConfirm = {
                showNotificationPermission = false
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
              },
            )
          }
        }
      }
    }
  }

  /**
   * The system splash screen stays until the first frame is drawn. Holding that frame until Today
   * has loaded its fasting state means the first thing shown is the whole screen, not a bare header
   * that fills in a moment later.
   */
  private fun keepSplashScreenUntilTodayIsReady() {
    val content: View = findViewById(android.R.id.content)
    content.viewTreeObserver.addOnPreDrawListener(
      object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
          if (!isTodayReady) return false
          content.viewTreeObserver.removeOnPreDrawListener(this)
          return true
        }
      }
    )
  }
}
