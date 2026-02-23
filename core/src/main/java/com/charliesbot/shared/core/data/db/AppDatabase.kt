package com.charliesbot.shared.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FastingRecord::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fastingRecordDao(): FastingRecordDao
}