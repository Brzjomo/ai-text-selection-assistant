package top.brzjomo.aitextselectionassistant.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import top.brzjomo.aitextselectionassistant.AITextSelectionAssistantApplication
import top.brzjomo.aitextselectionassistant.data.local.ApiProvider
import top.brzjomo.aitextselectionassistant.data.local.ProviderType
import top.brzjomo.aitextselectionassistant.data.local.ApiConfig
import top.brzjomo.aitextselectionassistant.data.local.UserPreferences
import top.brzjomo.aitextselectionassistant.data.repository.ApiProviderRepository

sealed interface ApiProviderUiState {
    data class Success(val providers: List<ApiProvider>) : ApiProviderUiState
    object Loading : ApiProviderUiState
    data class Error(val message: String) : ApiProviderUiState
}

data class ApiProviderEditState(
    val provider: ApiProvider = ApiProvider(
        name = "",
        providerType = ProviderType.OPENAI,
        baseUrl = "https://api.openai.com/v1/",
        apiKey = "",
        model = "gpt-4o-mini",
        enableStreaming = true,
        maxTokens = 128000,
        temperature = 0.7,
        enableAdvancedParams = false,
        topP = 1.0,
        customParameters = null,
        isDefault = false
    ),
    val isEditing: Boolean = false
)

class ApiProviderViewModel(
    private val context: Context
) : ViewModel() {
    private val repository = AITextSelectionAssistantApplication.getAppContainer(context).apiProviderRepository

    private val _uiState = MutableStateFlow<ApiProviderUiState>(ApiProviderUiState.Loading)
    val uiState: StateFlow<ApiProviderUiState> = _uiState.asStateFlow()

    private val _editState = MutableStateFlow(ApiProviderEditState())
    val editState: StateFlow<ApiProviderEditState> = _editState.asStateFlow()

    init {
        viewModelScope.launch {
            // migrateFromOldConfig()  // 已禁用自动迁移，避免自动创建"从旧配置升级"服务商
            loadProviders()
        }
    }

    fun loadProviders() {
        viewModelScope.launch {
            try {
                repository.getAllProviders().collect { providers ->
                    _uiState.value = ApiProviderUiState.Success(providers)
                }
            } catch (e: Exception) {
                _uiState.value = ApiProviderUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    fun startAddProvider() {
        _editState.value = ApiProviderEditState(
            provider = ApiProvider(
                name = "",
                providerType = ProviderType.OPENAI,
                baseUrl = "https://api.openai.com/v1/",
                apiKey = "",
                model = "gpt-4o-mini",
                enableStreaming = true,
                maxTokens = 128000,
                temperature = 0.7,
                enableAdvancedParams = false,
                topP = 1.0,
                customParameters = null,
                isDefault = false
            ),
            isEditing = true
        )
    }

    fun startEditProvider(provider: ApiProvider) {
        _editState.value = ApiProviderEditState(
            provider = provider.copy(),
            isEditing = true
        )
    }

    fun loadProvider(id: Long) {
        viewModelScope.launch {
            try {
                val provider = repository.getProviderById(id)
                if (provider != null) {
                    _editState.value = ApiProviderEditState(
                        provider = provider.copy(),
                        isEditing = true
                    )
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun updateEditProvider(provider: ApiProvider) {
        _editState.update { currentState ->
            currentState.copy(provider = provider)
        }
    }

    fun saveProvider() {
        viewModelScope.launch {
            val currentProvider = _editState.value.provider
            try {
                if (currentProvider.id == 0L) {
                    repository.insertProvider(currentProvider)
                } else {
                    repository.updateProvider(currentProvider)
                }
                _editState.value = ApiProviderEditState()
                loadProviders()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun deleteProvider(provider: ApiProvider) {
        viewModelScope.launch {
            try {
                repository.deleteProvider(provider)
                loadProviders()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun setDefaultProvider(id: Long) {
        viewModelScope.launch {
            try {
                repository.setDefaultProvider(id)
                loadProviders()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private suspend fun migrateFromOldConfig() {
        val appContainer = AITextSelectionAssistantApplication.getAppContainer(context)
        val userPreferences = appContainer.userPreferences

        // 检查是否有现有的提供商
        val existingProviders = repository.getAllProviders().firstOrNull() ?: emptyList()
        if (existingProviders.isNotEmpty()) {
            return  // 已经有提供商，无需迁移
        }

        // 获取旧的配置
        val oldConfig = userPreferences.apiConfigFlow.firstOrNull() ?: ApiConfig()
        if (oldConfig.apiKey.isBlank() && oldConfig.baseUrl.isBlank()) {
            return  // 旧的配置也是空的，无需迁移
        }

        // 根据旧的配置创建新的提供商
        val providerType = when {
            oldConfig.baseUrl.contains("openai", ignoreCase = true) -> ProviderType.OPENAI
            oldConfig.baseUrl.contains("deepseek", ignoreCase = true) -> ProviderType.DEEPSEEK
            oldConfig.baseUrl.contains("ollama", ignoreCase = true) -> ProviderType.OLLAMA
            else -> ProviderType.CUSTOM
        }

        val newProvider = ApiProvider(
            name = "从旧配置迁移",
            providerType = providerType,
            baseUrl = oldConfig.baseUrl,
            apiKey = oldConfig.apiKey.takeIf { it.isNotBlank() },
            model = oldConfig.model,
            enableStreaming = oldConfig.enableStreaming,
            maxTokens = oldConfig.maxTokens,
            temperature = oldConfig.temperature,
            isDefault = true
        )

        try {
            repository.insertProvider(newProvider)
            // 迁移成功后清除旧的配置
            userPreferences.clearApiConfig()
        } catch (e: Exception) {
            // 插入失败，保留旧配置以便下次重试
            // 可以记录日志或忽略异常
        }
    }

    fun cancelEdit() {
        _editState.value = ApiProviderEditState()
    }
}