package top.brzjomo.aitextselectionassistant.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import kotlinx.serialization.Serializable

@Entity(tableName = "api_providers")
data class ApiProvider(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "provider_type")
    val providerType: ProviderType,

    @ColumnInfo(name = "base_url")
    val baseUrl: String,

    @ColumnInfo(name = "api_key")
    val apiKey: String?,

    @ColumnInfo(name = "model")
    val model: String,

    @ColumnInfo(name = "enable_streaming")
    val enableStreaming: Boolean = true,

    @ColumnInfo(name = "max_tokens")
    val maxTokens: Int = 2000,

    @ColumnInfo(name = "temperature")
    val temperature: Double = 0.7,

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isValid: Boolean get() = when (providerType) {
        ProviderType.OLLAMA -> baseUrl.isNotBlank() && model.isNotBlank()
        else -> baseUrl.isNotBlank() && apiKey?.isNotBlank() == true && model.isNotBlank()
    }

    fun toApiConfig(): ApiConfig {
        return ApiConfig(
            apiKey = apiKey ?: "",
            baseUrl = baseUrl,
            model = model,
            enableStreaming = enableStreaming,
            maxTokens = maxTokens,
            temperature = temperature
        )
    }

    /**
     * 获取修正后的base URL，确保格式正确
     * 1. 移除末尾的"/chat/completions"路径（如果存在）
     * 2. 确保以斜杠结尾
     * 3. 对于DeepSeek，确保包含/v1/路径
     */
    fun getNormalizedBaseUrl(): String {
        var url = baseUrl.trim()

        // 如果URL包含/chat/completions，移除它（用户可能错误地包含了完整路径）
        if (url.contains("/chat/completions")) {
            url = url.substringBefore("/chat/completions")
        }

        // 确保以斜杠结尾
        if (!url.endsWith("/")) {
            url += "/"
        }

        // 对于DeepSeek，确保包含/v1/路径
        if (providerType == ProviderType.DEEPSEEK && !url.contains("/v1/")) {
            // 如果URL已经是api.deepseek.com，添加/v1/
            if (url.contains("api.deepseek.com")) {
                // 移除可能的末尾斜杠，然后添加/v1/
                url = url.removeSuffix("/") + "/v1/"
            }
        }

        return url
    }
}