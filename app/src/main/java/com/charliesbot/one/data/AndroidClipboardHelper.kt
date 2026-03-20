package com.charliesbot.one.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.charliesbot.shared.core.abstraction.ClipboardHelper

class AndroidClipboardHelper(private val context: Context) : ClipboardHelper {
  override fun copy(label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
  }
}
