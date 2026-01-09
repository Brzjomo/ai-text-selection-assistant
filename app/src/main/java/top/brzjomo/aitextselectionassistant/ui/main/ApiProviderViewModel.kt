package top.brzjomo.aitextselectionassistant.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.brzjomo.aitextselectionassistant.AITextSelectionAssistantApplication
import top.brzjomo.aitextselectionassistant.data.local.ApiProvider
import top.brzjomo.aitextselectionassistant.data.local.ProviderType

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
    context: Context
) : ViewModel() {
    private val repository = AITextSelectionAssistantApplication.getAppContainer(context).apiProviderRepository

    private val _uiState = MutableStateFlow<ApiProviderUiState>(ApiProviderUiState.Loading)
    val uiState: StateFlow<ApiProviderUiState> = _uiState.asStateFlow()

    private val _editState = MutableStateFlow(ApiProviderEditState())
    val editState: StateFlow<ApiProviderEditState> = _editState.asStateFlow()

    init {
        viewModelScope.launch {
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

    fun cancelEdit() {
        _editState.value = ApiProviderEditState()
    }
}