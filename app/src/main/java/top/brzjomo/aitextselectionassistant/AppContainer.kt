package top.brzjomo.aitextselectionassistant

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import top.brzjomo.aitextselectionassistant.data.local.AppDatabase
import top.brzjomo.aitextselectionassistant.data.local.UserPreferences
import top.brzjomo.aitextselectionassistant.data.repository.PromptTemplateRepository

// DataStore 扩展属性
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppContainer(private val context: Context) {
    // 数据库实例
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ai-text-selection-assistant.db"
        ).build()
    }

    // DataStore 实例
    private val dataStore: DataStore<Preferences> by lazy {
        context.dataStore
    }

    // 用户偏好设置
    val userPreferences: UserPreferences by lazy {
        UserPreferences(dataStore)
    }

    // PromptTemplate DAO
    private val promptTemplateDao by lazy {
        database.promptTemplateDao()
    }

    // Repository
    val promptTemplateRepository: PromptTemplateRepository by lazy {
        PromptTemplateRepository(promptTemplateDao)
    }
}