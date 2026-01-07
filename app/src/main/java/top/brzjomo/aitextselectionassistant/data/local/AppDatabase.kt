package top.brzjomo.aitextselectionassistant.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PromptTemplate::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun promptTemplateDao(): PromptTemplateDao
}