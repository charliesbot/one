package com.charliesbot.shared.core.abstraction

import com.charliesbot.shared.core.data.db.FastingRecord

interface HistoryExporter {
  suspend fun export(records: List<FastingRecord>): Result<String>
}
