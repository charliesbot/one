package com.charliesbot.onewearos.presentation.data

import android.content.Context
import com.charliesbot.shared.core.domain.platform.StringProvider

class WearStringProvider(private val context: Context) : StringProvider {
  override fun getString(resourceId: String): String {
    val resourceIdInt = context.resources.getIdentifier(resourceId, "string", context.packageName)
    return context.getString(resourceIdInt)
  }
}
