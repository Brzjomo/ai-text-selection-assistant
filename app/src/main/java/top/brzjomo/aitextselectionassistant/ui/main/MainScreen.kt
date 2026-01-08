package top.brzjomo.aitextselectionassistant.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import top.brzjomo.aitextselectionassistant.AITextSelectionAssistantApplication

sealed class MainScreenRoute(val route: String) {
    object Home : MainScreenRoute("home")
    object ApiConfig : MainScreenRoute("api_config")
    object PromptList : MainScreenRoute("prompt_list")
    object PromptEdit : MainScreenRoute("prompt_edit/{templateId}") {
        fun createRoute(templateId: Long = 0) = "prompt_edit/$templateId"
    }
    object ApiProviderEdit : MainScreenRoute("api_provider_edit/{providerId}") {
        fun createRoute(providerId: Long = 0) = "api_provider_edit/$providerId"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = MainScreenRoute.Home.route
    ) {
        composable(MainScreenRoute.Home.route) {
            HomeScreen(
                onApiConfigClick = { navController.navigate(MainScreenRoute.ApiConfig.route) },
                onPromptManageClick = { navController.navigate(MainScreenRoute.PromptList.route) }
            )
        }

        composable(MainScreenRoute.ApiConfig.route) {
            ApiProviderListScreen(
                onEditProvider = { providerId ->
                    navController.navigate(MainScreenRoute.ApiProviderEdit.createRoute(providerId))
                }
            )
        }

        composable(MainScreenRoute.PromptList.route) {
            PromptListScreen(
                onEditTemplate = { templateId ->
                    navController.navigate(MainScreenRoute.PromptEdit.createRoute(templateId))
                }
            )
        }

        composable(MainScreenRoute.PromptEdit.route) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId")?.toLongOrNull() ?: 0L
            PromptEditScreen(
                templateId = templateId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(MainScreenRoute.ApiProviderEdit.route) { backStackEntry ->
            val providerId = backStackEntry.arguments?.getString("providerId")?.toLongOrNull() ?: 0L
            ApiProviderEditScreen(
                providerId = providerId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    onApiConfigClick: () -> Unit,
    onPromptManageClick: () -> Unit
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

    // 获取当前默认服务商
    val currentProvider = when (val state = uiState) {
        is ApiProviderUiState.Success -> {
            state.providers.find { it.isDefault } ?: state.providers.firstOrNull()
        }
        ApiProviderUiState.Loading -> null
        is ApiProviderUiState.Error -> null
    }

    LaunchedEffect(Unit) {
        viewModel.loadProviders()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 划词助手") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {


            Spacer(modifier = Modifier.height(56.dp))

            // 当前服务商卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                onClick = onApiConfigClick
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Api,
                        contentDescription = "当前服务商",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (currentProvider != null) "当前服务商" else "未配置服务商",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (currentProvider != null) {
                                "${currentProvider.name} (${currentProvider.providerType.name})"
                            } else {
                                "点击配置您的 API 服务商"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (currentProvider != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "模型: ${currentProvider.model}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // 下拉箭头指示可点击
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "点击配置",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FeatureCard(
                title = "API 配置",
                description = "配置您的 LLM API 密钥和参数",
                icon = Icons.Default.Api,
                onClick = onApiConfigClick
            )

            FeatureCard(
                title = "Prompt 模板管理",
                description = "管理 AI 处理文本的 Prompt 模板",
                icon = Icons.AutoMirrored.Filled.List,
                onClick = onPromptManageClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "使用说明",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. 在任意应用中选中文本\n2. 点击分享或更多选项\n3. 选择 \"AI 助手\"\n4. 使用模板处理文本",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}