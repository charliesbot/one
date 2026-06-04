package com.charliesbot.shared.core.domain.platform

import com.charliesbot.shared.core.models.FastingRecord

interface HistoryExporter {
  suspend fun export(records: List<FastingRecord>): Result<String>
}
