package top.brzjomo.aitextselectionassistant.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import top.brzjomo.aitextselectionassistant.AITextSelectionAssistantApplication
import top.brzjomo.aitextselectionassistant.data.local.ApiProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiProviderListScreen(
    onEditProvider: (Long) -> Unit = {}
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
    val uiState by viewModel.uiState.collectAsState()
    val providers = when (val state = uiState) {
        is ApiProviderUiState.Success -> state.providers
        ApiProviderUiState.Loading -> emptyList()
        is ApiProviderUiState.Error -> emptyList()
    }

    LaunchedEffect(Unit) {
        viewModel.loadProviders()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API 服务商管理") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEditProvider(0L) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加服务商")
            }
        }
    ) { paddingValues ->
        if (providers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无服务商配置，点击右下角按钮添加")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(providers) { provider ->
                    ApiProviderCard(
                        provider = provider,
                        onEdit = { onEditProvider(provider.id) },
                        onDelete = { viewModel.deleteProvider(provider) },
                        onSetDefault = { viewModel.setDefaultProvider(provider.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ApiProviderCard(
    provider: ApiProvider,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = provider.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (provider.isDefault) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "默认",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = "${provider.providerType.name} - ${provider.model}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row {
                    if (!provider.isDefault) {
                        IconButton(
                            onClick = onSetDefault,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.RadioButtonUnchecked,
                                contentDescription = "设为默认",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = provider.baseUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (provider.apiKey?.isNotBlank() == true) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "API Key: ******",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "流式: ${if (provider.enableStreaming) "是" else "否"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "最大tokens: ${provider.maxTokens}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "温度: ${provider.temperature}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}