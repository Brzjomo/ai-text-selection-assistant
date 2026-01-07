package top.brzjomo.aitextselectionassistant.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

sealed class MainScreenRoute(val route: String) {
    object Home : MainScreenRoute("home")
    object ApiConfig : MainScreenRoute("api_config")
    object PromptList : MainScreenRoute("prompt_list")
    object PromptEdit : MainScreenRoute("prompt_edit/{templateId}") {
        fun createRoute(templateId: Long = 0) = "prompt_edit/$templateId"
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
            ApiConfigScreen()
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    onApiConfigClick: () -> Unit,
    onPromptManageClick: () -> Unit
) {
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
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "AI 划词助手",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "跨应用文本 AI 处理工具",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

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