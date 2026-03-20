package com.charliesbot.one.data

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.charliesbot.shared.core.abstraction.HistoryExporter
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.data.db.FastingRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AndroidHistoryExporter(private val context: Context) : HistoryExporter {
  override suspend fun export(records: List<FastingRecord>): Result<String> =
    try {
      val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
      val fileName = "fasting_history_$timestamp.csv"

      val contentValues =
        ContentValues().apply {
          put(MediaStore.Downloads.DISPLAY_NAME, fileName)
          put(MediaStore.Downloads.MIME_TYPE, "text/csv")
          put(MediaStore.Downloads.IS_PENDING, 1)
        }

      val resolver = context.contentResolver
      val uri =
        resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
          ?: return Result.failure(Exception("Failed to create file in Downloads"))

      resolver.openOutputStream(uri)?.use { outputStream ->
        outputStream.bufferedWriter().use { writer ->
          writer.append("Start Time,End Time,Duration (hours),Goal\n")
          val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
          records.forEach { record ->
            val startDate = dateFormat.format(Date(record.startTimeEpochMillis))
            val endDate = dateFormat.format(Date(record.endTimeEpochMillis))
            val durationHours =
              (record.endTimeEpochMillis - record.startTimeEpochMillis) / (1000 * 60 * 60)
            writer.append("$startDate,$endDate,$durationHours,${record.fastingGoalId}\n")
          }
        }
      }

      contentValues.clear()
      contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
      resolver.update(uri, contentValues, null, null)

      Log.d(LOG_TAG, "AndroidHistoryExporter: Export successful - saved to Downloads/$fileName")
      Result.success(fileName)
    } catch (e: Exception) {
      Log.e(LOG_TAG, "AndroidHistoryExporter: Export failed", e)
      Result.failure(e)
    }
}
