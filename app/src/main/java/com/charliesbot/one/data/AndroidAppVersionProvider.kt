package com.charliesbot.one.data

import android.content.Context
import android.util.Log
import com.charliesbot.shared.core.abstraction.AppVersionProvider
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG

class AndroidAppVersionProvider(private val context: Context) : AppVersionProvider {
  override val versionName: String =
    try {
      context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
    } catch (e: Exception) {
      Log.e(LOG_TAG, "AndroidAppVersionProvider: Failed to get version name", e)
      "Unknown"
    }
}
