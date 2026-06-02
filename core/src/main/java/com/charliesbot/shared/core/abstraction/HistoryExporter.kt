package com.charliesbot.shared.core.abstraction

import com.charliesbot.shared.core.models.FastingRecord

interface HistoryExporter {
  suspend fun export(records: List<FastingRecord>): Result<String>
}
