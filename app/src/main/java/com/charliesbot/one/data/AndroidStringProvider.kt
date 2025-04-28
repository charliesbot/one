package com.charliesbot.one.data

import android.content.Context
import com.charliesbot.shared.core.abstraction.StringProvider

class AndroidStringProvider(private val context: Context): StringProvider {
    override fun getString(resourceId: String): String {
        val resourceIdInt = context.resources.getIdentifier(resourceId, "string", context.packageName)
        return context.getString(resourceIdInt)
    }
}