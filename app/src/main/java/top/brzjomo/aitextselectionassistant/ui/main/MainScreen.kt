package top.brzjomo.aitextselectionassistant.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import top.brzjomo.aitextselectionassistant.AITextSelectionAssistantApplication
import top.brzjomo.aitextselectionassistant.data.local.UserPreferences
import kotlinx.coroutines.launch
import android.provider.Settings
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import top.brzjomo.aitextselectionassistant.service.ClipboardMonitorService

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
    object Settings : MainScreenRoute("settings")
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
                onPromptManageClick = { navController.navigate(MainScreenRoute.PromptList.route) },
                onSettingsClick = { navController.navigate(MainScreenRoute.Settings.route) }
            )
        }

        composable(MainScreenRoute.ApiConfig.route) {
            ApiProviderListScreen(
                onEditProvider = { providerId ->
                    navController.navigate(MainScreenRoute.ApiProviderEdit.createRoute(providerId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(MainScreenRoute.PromptList.route) {
            PromptListScreen(
                onEditTemplate = { templateId ->
                    navController.navigate(MainScreenRoute.PromptEdit.createRoute(templateId))
                },
                onBack = { navController.popBackStack() }
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

        composable(MainScreenRoute.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    onApiConfigClick: () -> Unit,
    onPromptManageClick: () -> Unit,
    onSettingsClick: () -> Unit
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
    var showProviderDialog by remember { mutableStateOf(false) }

    // 获取当前默认服务商
    val currentProvider = when (val state = uiState) {
        is ApiProviderUiState.Success -> {
            state.providers.find { it.isDefault } ?: state.providers.firstOrNull()
        }
        ApiProviderUiState.Loading -> null
        is ApiProviderUiState.Error -> null
    }
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
                title = { Text("AI 划词助手") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "设置"
                        )
                    }
                }
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
                onClick = { showProviderDialog = true }
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
                        text = "1. 在任意应用中选中文本\n2. 点击分享或更多选项\n3. 选择 \"AI 划词助手\"\n4. 使用模板处理文本",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showProviderDialog) {
            AlertDialog(
                onDismissRequest = { showProviderDialog = false },
                title = { Text("选择当前服务商") },
                text = {
                    Column {
                        providers.forEach { provider ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable {
                                        viewModel.setDefaultProvider(provider.id)
                                        showProviderDialog = false
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (provider.isDefault) {
                                    Box(
                                        modifier = Modifier.size(28.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = "已选中",
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier.size(28.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Circle,
                                            contentDescription = "未选中",
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = provider.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "${provider.providerType.name} - ${provider.model}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showProviderDialog = false }) {
                        Text("取消")
                    }
                }
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as AITextSelectionAssistantApplication).appContainer
    val userPreferences = appContainer.userPreferences
    val appConfig by userPreferences.appConfigFlow.collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    val versionName: String = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "未知"
        }.getOrElse { "未知" }
    }

    // 检查无障碍服务是否启用
    val isAccessibilityServiceEnabled = remember {
        runCatching {
            val serviceName = "${context.packageName}/${ClipboardMonitorService::class.java.name}"
            val setting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            setting?.contains(serviceName) == true
        }.getOrElse { false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 自动弹出悬浮窗开关
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "复制后自动弹出悬浮窗",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "在其他应用选择文本复制后，自动弹出AI处理悬浮窗",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = appConfig?.enableAutoClipboard ?: false,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch {
                                userPreferences.setAutoClipboardEnabled(enabled)
                            }
                        }
                    )
                }
            }

            // 无障碍服务状态
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAccessibilityServiceEnabled) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isAccessibilityServiceEnabled) Icons.Filled.Info else Icons.Filled.Warning,
                            contentDescription = "无障碍服务状态",
                            tint = if (isAccessibilityServiceEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isAccessibilityServiceEnabled) "无障碍服务已启用" else "无障碍服务未启用",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isAccessibilityServiceEnabled) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                            Text(
                                text = if (isAccessibilityServiceEnabled) {
                                    "服务正在运行，可以监控复制操作"
                                } else {
                                    "需要开启无障碍服务权限才能使用自动弹出功能"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!isAccessibilityServiceEnabled) {
                        Button(
                            onClick = {
                                // 跳转到无障碍设置页面
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("前往开启无障碍服务")
                        }
                    }
                }
            }

            // 应用信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "应用信息",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // 版本号
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("版本")
                        Text(versionName)
                    }

                    // 开发者
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("开发者")
                        Text("brzjomo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // GitHub
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("GitHub")
                        Text(
                            text = "github.com/brzjomo",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

        }
    }
}