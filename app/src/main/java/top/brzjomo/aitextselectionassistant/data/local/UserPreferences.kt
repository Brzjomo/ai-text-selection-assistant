package top.brzjomo.aitextselectionassistant.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferences(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val API_KEY = stringPreferencesKey("api_key")
        private val BASE_URL = stringPreferencesKey("base_url")
        private val MODEL = stringPreferencesKey("model")
        private val ENABLE_STREAMING = booleanPreferencesKey("enable_streaming")
        private val MAX_TOKENS = stringPreferencesKey("max_tokens")
        private val TEMPERATURE = stringPreferencesKey("temperature")
    }

    val apiConfigFlow: Flow<ApiConfig> = dataStore.data.map { preferences ->
        ApiConfig(
            apiKey = preferences[API_KEY] ?: "",
            baseUrl = preferences[BASE_URL] ?: "https://api.openai.com/v1/",
            model = preferences[MODEL] ?: "gpt-4o-mini",
            enableStreaming = preferences[ENABLE_STREAMING] ?: true,
            maxTokens = preferences[MAX_TOKENS]?.toIntOrNull() ?: 2000,
            temperature = preferences[TEMPERATURE]?.toDoubleOrNull() ?: 0.7
        )
    }

    suspend fun updateApiConfig(apiConfig: ApiConfig) {
        dataStore.edit { preferences ->
            preferences[API_KEY] = apiConfig.apiKey
            preferences[BASE_URL] = apiConfig.baseUrl
            preferences[MODEL] = apiConfig.model
            preferences[ENABLE_STREAMING] = apiConfig.enableStreaming
            preferences[MAX_TOKENS] = apiConfig.maxTokens.toString()
            preferences[TEMPERATURE] = apiConfig.temperature.toString()
        }
    }

    suspend fun clearApiConfig() {
        dataStore.edit { preferences ->
            preferences.remove(API_KEY)
            preferences.remove(BASE_URL)
            preferences.remove(MODEL)
            preferences.remove(ENABLE_STREAMING)
            preferences.remove(MAX_TOKENS)
            preferences.remove(TEMPERATURE)
        }
    }
}

data class ApiConfig(
    val apiKey: String = "",
    val baseUrl: String = "https://api.openai.com/v1/",
    val model: String = "gpt-4o-mini",
    val enableStreaming: Boolean = true,
    val maxTokens: Int = 2000,
    val temperature: Double = 0.7
) {
    val isValid: Boolean get() = apiKey.isNotBlank() && baseUrl.isNotBlank()
}