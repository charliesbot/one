package com.charliesbot.one.widget.wear

import androidx.glance.wear.GlanceWearWidget
import androidx.glance.wear.GlanceWearWidgetService

class OneWearWidgetService : GlanceWearWidgetService() {
  override val widget: GlanceWearWidget = OneWearWidget()
}
