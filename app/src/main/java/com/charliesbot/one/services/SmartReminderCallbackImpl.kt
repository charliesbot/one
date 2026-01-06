package com.charliesbot.one.services

import android.content.Context
import com.charliesbot.one.notifications.SmartReminderWorker
import com.charliesbot.shared.core.services.SmartReminderCallback

class SmartReminderCallbackImpl(
    private val context: Context
) : SmartReminderCallback {
    
    override fun onSmartReminderSettingsChanged() {
        // Trigger an immediate recalculation and sync
        SmartReminderWorker.triggerImmediateRun(context)
    }
}

