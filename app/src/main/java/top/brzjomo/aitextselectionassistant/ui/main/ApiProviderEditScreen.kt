package top.brzjomo.aitextselectionassistant.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import top.brzjomo.aitextselectionassistant.AITextSelectionAssistantApplication
import top.brzjomo.aitextselectionassistant.data.local.ProviderType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiProviderEditScreen(
    providerId: Long? = null,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel = viewModel<ApiProviderViewModel>(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ApiProviderViewModel::class.java)) {
                    return ApiProviderViewModel(context) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    )
    val editState by viewModel.editState.collectAsState()

    LaunchedEffect(providerId) {
        when {
            providerId == null || providerId == 0L -> {
                viewModel.startAddProvider()
            }
            providerId > 0 -> {
                viewModel.loadProvider(providerId)
            }
        }
    }

    EditProviderForm(
        provider = editState.provider,
        onProviderChange = { updatedProvider ->
            viewModel.updateEditProvider(updatedProvider)
        },
        onSave = {
            viewModel.saveProvider()
            onBack()
        },
        onCancel = {
            viewModel.cancelEdit()
            onBack()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProviderForm(
    provider: top.brzjomo.aitextselectionassistant.data.local.ApiProvider,
    onProviderChange: (top.brzjomo.aitextselectionassistant.data.local.ApiProvider) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(provider.name) }
    var providerType by remember { mutableStateOf(provider.providerType) }
    var baseUrl by remember { mutableStateOf(provider.baseUrl) }
    var apiKey by remember { mutableStateOf(provider.apiKey ?: "") }
    var model by remember { mutableStateOf(provider.model) }
    var enableStreaming by remember { mutableStateOf(provider.enableStreaming) }
    var maxTokens by remember { mutableStateOf(provider.maxTokens.toString()) }
    var temperature by remember { mutableStateOf(provider.temperature.toString()) }
    var isDefault by remember { mutableStateOf(provider.isDefault) }
    var enableAdvancedParams by remember { mutableStateOf(provider.enableAdvancedParams) }
    var topP by remember { mutableStateOf(provider.topP.toString()) }
    var customParameters by remember { mutableStateOf(provider.customParameters ?: "") }
    var showApiKey by remember { mutableStateOf(false) }

    LaunchedEffect(provider) {
        name = provider.name
        providerType = provider.providerType
        baseUrl = provider.baseUrl
        apiKey = provider.apiKey ?: ""
        model = provider.model
        enableStreaming = provider.enableStreaming
        maxTokens = provider.maxTokens.toString()
        temperature = provider.temperature.toString()
        isDefault = provider.isDefault
        enableAdvancedParams = provider.enableAdvancedParams
        topP = provider.topP.toString()
        customParameters = provider.customParameters ?: ""
    }

    LaunchedEffect(
        name, providerType, baseUrl, apiKey, model,
        enableStreaming, maxTokens, temperature, isDefault,
        enableAdvancedParams, topP, customParameters
    ) {
        val newProvider = provider.copy(
            name = name,
            providerType = providerType,
            baseUrl = baseUrl,
            apiKey = if (apiKey.isNotBlank()) apiKey else null,
            model = model,
            enableStreaming = enableStreaming,
            maxTokens = maxTokens.toIntOrNull() ?: 2000,
            temperature = temperature.toDoubleOrNull() ?: 0.7,
            isDefault = isDefault,
            enableAdvancedParams = enableAdvancedParams,
            topP = topP.toDoubleOrNull() ?: 1.0,
            customParameters = if (customParameters.isNotBlank()) customParameters else null
        )
        onProviderChange(newProvider)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (provider.id == 0L) "添加服务商" else "编辑服务商") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSave,
                        enabled = provider.isValid
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = @Composable { Text("服务商名称") },
                placeholder = @Composable { Text("例如：OpenAI 官方") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            // 服务商类型选择
            var expanded by remember { mutableStateOf(false) }
            Column {
                Text(
                    text = "服务商类型",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = providerType.name,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ProviderType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    providerType = type
                                    expanded = false
                                    // 根据类型设置默认值
                                    when (type) {
                                        ProviderType.OPENAI -> {
                                            baseUrl = "https://api.openai.com/v1/"
                                            model = "gpt-4o-mini"
                                        }
                                        ProviderType.DEEPSEEK -> {
                                            baseUrl = "https://api.deepseek.com/v1/"
                                            model = "deepseek-chat"
                                        }
                                        ProviderType.OLLAMA -> {
                                            baseUrl = "http://192.168.1.100:11434/v1/"
                                            model = "llama3.2"
                                            apiKey = ""
                                        }
                                        ProviderType.CUSTOM -> {
                                            // 保持现有值
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = @Composable { Text("Base URL") },
                placeholder = @Composable { Text("https://api.openai.com/v1/") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )

            // Base URL格式提示
            Text(
                text = when (providerType) {
                    ProviderType.OPENAI -> "OpenAI API格式: https://api.openai.com/v1/"
                    ProviderType.DEEPSEEK -> "DeepSeek API格式: https://api.deepseek.com/v1/"
                    ProviderType.OLLAMA -> "Ollama本地服务格式: http://IP地址:11434/v1/ （如: http://192.168.1.100:11434/v1/）"
                    ProviderType.CUSTOM -> "自定义API端点格式"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )

            if (providerType != ProviderType.OLLAMA) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = @Composable { Text("API Key") },
                    placeholder = @Composable { Text("输入您的 API 密钥") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = @Composable {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showApiKey) "隐藏密钥" else "显示密钥"
                            )
                        }
                    }
                )
            }

            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = @Composable { Text("模型") },
                placeholder = @Composable { Text("gpt-4o-mini") },
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = enableAdvancedParams,
                    onCheckedChange = { enableAdvancedParams = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "启用高级参数",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (enableAdvancedParams) {
                OutlinedTextField(
                    value = maxTokens,
                    onValueChange = { maxTokens = it },
                    label = @Composable { Text("最大 tokens") },
                    placeholder = @Composable { Text("2000") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = temperature,
                    onValueChange = { temperature = it },
                    label = @Composable { Text("温度 (0.0-2.0)") },
                    placeholder = @Composable { Text("0.7") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = topP,
                    onValueChange = { topP = it },
                    label = @Composable { Text("Top-P (0.0-1.0)") },
                    placeholder = @Composable { Text("1.0") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Text(
                    text = "自定义参数（每行一个参数，格式如：frequency_penalty:0.5）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                OutlinedTextField(
                    value = customParameters,
                    onValueChange = { customParameters = it },
                    label = @Composable { Text("自定义参数") },
                    placeholder = @Composable { Text("frequency_penalty:0.5\npresence_penalty:0.5\nstop:END") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 4
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isDefault,
                    onCheckedChange = { isDefault = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "设为默认服务商",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = onSave,
                enabled = provider.isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存服务商")
            }
        }
    }
}