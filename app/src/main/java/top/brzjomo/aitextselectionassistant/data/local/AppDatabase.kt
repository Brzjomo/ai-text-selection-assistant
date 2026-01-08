package top.brzjomo.aitextselectionassistant.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PromptTemplate::class, ApiProvider::class],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun promptTemplateDao(): PromptTemplateDao
    abstract fun apiProviderDao(): ApiProviderDao
}