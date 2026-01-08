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
}