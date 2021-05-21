package com.mehul.textrecognizer.scans

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Scan::class],
    exportSchema = false,
    version = 1
)
abstract class ScanDatabase(): RoomDatabase() {

    abstract fun getScanDao(): ScanDao

    companion object {
        private const val databaseName = "scan_db"

        @Volatile
        private var instance: ScanDatabase? = null

        private val LOCK = Any()

        operator fun invoke(context: Context) = instance ?: synchronized(LOCK) {
            instance ?: buildDataBase(context).also {
                instance = it
            }
        }

        private fun buildDataBase(context: Context) = Room.databaseBuilder(
            context.applicationContext,
            ScanDatabase::class.java,
            databaseName
        ).build()
    }
}