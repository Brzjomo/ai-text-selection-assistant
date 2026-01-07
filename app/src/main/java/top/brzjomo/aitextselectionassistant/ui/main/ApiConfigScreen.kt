package top.brzjomo.aitextselectionassistant.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.brzjomo.aitextselectionassistant.AITextSelectionAssistantApplication
import top.brzjomo.aitextselectionassistant.data.local.ApiConfig
import top.brzjomo.aitextselectionassistant.data.local.UserPreferences

@Composable
fun ApiConfigScreen(
    viewModel: ApiConfigViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val context = LocalContext.current
                return ApiConfigViewModel(context) as T
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "API 配置",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        ApiConfigForm(
            apiConfig = uiState.apiConfig,
            onApiConfigChanged = { updatedConfig ->
                viewModel.updateLocalConfig(updatedConfig)
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.saveConfig() },
            enabled = uiState.apiConfig.isValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("保存配置")
        }

        if (uiState.isSaved) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "配置已保存",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ApiConfigForm(
    apiConfig: ApiConfig,
    onApiConfigChanged: (ApiConfig) -> Unit
) {
    var apiKey by rememberSaveable { mutableStateOf(apiConfig.apiKey) }
    var baseUrl by rememberSaveable { mutableStateOf(apiConfig.baseUrl) }
    var model by rememberSaveable { mutableStateOf(apiConfig.model) }
    var enableStreaming by rememberSaveable { mutableStateOf(apiConfig.enableStreaming) }
    var maxTokens by rememberSaveable { mutableStateOf(apiConfig.maxTokens.toString()) }
    var temperature by rememberSaveable { mutableStateOf(apiConfig.temperature.toString()) }
    var showApiKey by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(apiConfig) {
        apiKey = apiConfig.apiKey
        baseUrl = apiConfig.baseUrl
        model = apiConfig.model
        enableStreaming = apiConfig.enableStreaming
        maxTokens = apiConfig.maxTokens.toString()
        temperature = apiConfig.temperature.toString()
    }

    LaunchedEffect(apiKey, baseUrl, model, enableStreaming, maxTokens, temperature) {
        val newConfig = ApiConfig(
            apiKey = apiKey,
            baseUrl = baseUrl,
            model = model,
            enableStreaming = enableStreaming,
            maxTokens = maxTokens.toIntOrNull() ?: 2000,
            temperature = temperature.toDoubleOrNull() ?: 0.7
        )
        onApiConfigChanged(newConfig)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key") },
            placeholder = { Text("输入您的 API 密钥") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showApiKey = !showApiKey }) {
                    Icon(
                        imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showApiKey) "隐藏密钥" else "显示密钥"
                    )
                }
            }
        )

        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("Base URL") },
            placeholder = { Text("https://api.openai.com/v1/") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )

        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("模型") },
            placeholder = { Text("gpt-4o-mini") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = enableStreaming,
                onCheckedChange = { enableStreaming = it }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "启用流式输出",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        OutlinedTextField(
            value = maxTokens,
            onValueChange = { maxTokens = it },
            label = { Text("最大 tokens") },
            placeholder = { Text("2000") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        OutlinedTextField(
            value = temperature,
            onValueChange = { temperature = it },
            label = { Text("温度 (0.0-2.0)") },
            placeholder = { Text("0.7") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

data class ApiConfigUiState(
    val apiConfig: ApiConfig = ApiConfig(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class ApiConfigViewModel(
    context: android.content.Context
) : ViewModel() {
    private val userPreferences = AITextSelectionAssistantApplication.getAppContainer(context).userPreferences

    private val _uiState = MutableStateFlow(ApiConfigUiState())
    val uiState: StateFlow<ApiConfigUiState> = _uiState.asStateFlow()

    init {
        loadConfig()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            userPreferences.apiConfigFlow.collect { apiConfig ->
                _uiState.value = _uiState.value.copy(
                    apiConfig = apiConfig,
                    isLoading = false
                )
            }
        }
    }

    fun updateLocalConfig(apiConfig: ApiConfig) {
        _uiState.value = _uiState.value.copy(
            apiConfig = apiConfig,
            isSaved = false
        )
    }

    fun saveConfig() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                userPreferences.updateApiConfig(_uiState.value.apiConfig)
                _uiState.value = _uiState.value.copy(
                    isSaved = true,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "保存失败",
                    isLoading = false
                )
            }
        }
    }
}