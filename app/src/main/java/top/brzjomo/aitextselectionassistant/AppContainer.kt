package top.brzjomo.aitextselectionassistant

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import top.brzjomo.aitextselectionassistant.data.local.ApiConfig
import top.brzjomo.aitextselectionassistant.data.local.AppDatabase
import top.brzjomo.aitextselectionassistant.data.local.UserPreferences
import top.brzjomo.aitextselectionassistant.data.remote.LlmService
import top.brzjomo.aitextselectionassistant.data.remote.SseParser
import top.brzjomo.aitextselectionassistant.data.repository.PromptTemplateRepository
import java.util.concurrent.TimeUnit

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

    // Gson 实例
    val gson: Gson by lazy {
        GsonBuilder().create()
    }

    // SSE 解析器
    val sseParser: SseParser by lazy {
        SseParser(gson)
    }

    // 创建 LlmService 实例
    fun createLlmService(apiConfig: ApiConfig): LlmService {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)  // 流式响应需要更长的读取超时
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("Authorization", "Bearer ${apiConfig.apiKey}")
                    .header("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .apply {
                // 添加日志拦截器（仅调试模式）
                val isDebuggable = context.applicationInfo != null &&
                        (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                if (isDebuggable) {
                    val loggingInterceptor = HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                    addInterceptor(loggingInterceptor)
                }
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(apiConfig.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit.create(LlmService::class.java)
    }
}