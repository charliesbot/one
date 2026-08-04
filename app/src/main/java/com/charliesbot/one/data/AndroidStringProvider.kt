package com.charliesbot.one.data

import android.content.Context
import com.charliesbot.shared.core.domain.platform.StringKey
import com.charliesbot.shared.core.domain.platform.StringProvider
import com.charliesbot.shared.core.platform.getString

class AndroidStringProvider(private val context: Context) : StringProvider {
  override fun getString(key: StringKey): String = context.getString(key)
}
