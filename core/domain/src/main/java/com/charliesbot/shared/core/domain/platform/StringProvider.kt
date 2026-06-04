package com.charliesbot.shared.core.domain.platform

interface StringProvider {
  fun getString(resourceId: String): String
}
