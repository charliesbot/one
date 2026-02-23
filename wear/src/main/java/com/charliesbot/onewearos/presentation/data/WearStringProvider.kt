package com.charliesbot.onewearos.presentation.data

import android.content.Context
import com.charliesbot.shared.core.abstraction.StringProvider

class WearStringProvider(private val context: Context) : StringProvider {
    override fun getString(resourceId: String): String {
        val resourceIdInt =
            context.resources.getIdentifier(resourceId, "string", context.packageName)
        return context.getString(resourceIdInt)
    }
}
