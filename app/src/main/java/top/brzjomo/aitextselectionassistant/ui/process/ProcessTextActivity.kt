package top.brzjomo.aitextselectionassistant.ui.process

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import top.brzjomo.aitextselectionassistant.AITextSelectionAssistantApplication
import top.brzjomo.aitextselectionassistant.ViewModelFactory
import top.brzjomo.aitextselectionassistant.data.repository.PromptTemplateRepository
import top.brzjomo.aitextselectionassistant.ui.components.MarkdownText
import top.brzjomo.aitextselectionassistant.ui.process.ProcessTextEvent
import top.brzjomo.aitextselectionassistant.ui.theme.AITextSelectionAssistantTheme

class ProcessTextActivity : ComponentActivity() {

    private val viewModel: ProcessTextViewModel by viewModels(
        factoryProducer = {
            val appContainer = AITextSelectionAssistantApplication.getAppContainer(this)
            ViewModelFactory(appContainer, this)
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取选中的文本
        val selectedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""

        // Dialog 窗口配置
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.BOTTOM)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        setContent {
            var darkThemeOverride by remember { mutableStateOf<Boolean?>(null) }
            val darkTheme = darkThemeOverride ?: isSystemInDarkTheme()
            val systemDarkTheme = isSystemInDarkTheme()

            AITextSelectionAssistantTheme(
                darkTheme = darkTheme,
                dynamicColor = false // 禁用动态颜色以确保主题切换效果明显
            ) {
                ProcessTextScreen(
                    selectedText = selectedText,
                    viewModel = viewModel,
                    darkTheme = darkTheme,
                    onToggleTheme = {
                        darkThemeOverride = when (darkThemeOverride) {
                            null -> !systemDarkTheme // 从跟随系统切换到相反模式
                            true -> false // 从深色切换到浅色
                            false -> null // 从浅色切换回跟随系统
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ProcessTextScreen(
    selectedText: String,
    viewModel: ProcessTextViewModel,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val configuration = LocalConfiguration.current
    val maxHeight = (configuration.screenHeightDp * 0.8).dp

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 选中的模板ID状态
    var selectedTemplateId by remember { mutableLongStateOf(0L) }

    // 选中的文本展开状态（默认折叠）
    var isTextExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .heightIn(max = maxHeight),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI 划词助手",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // 主题切换按钮
                IconButton(
                    onClick = onToggleTheme
                ) {
                    Icon(
                        imageVector = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = if (darkTheme) "切换到浅色模式" else "切换到深色模式",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 选中的文本显示（可折叠）
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // 可点击的标题行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "选中的文本：",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        IconButton(
                            onClick = { isTextExpanded = !isTextExpanded },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isTextExpanded)
                                    Icons.Filled.ExpandLess
                                else
                                    Icons.Filled.ExpandMore,
                                contentDescription = if (isTextExpanded)
                                    "折叠选中的文本"
                                else
                                    "展开选中的文本",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 条件显示文本内容
                    if (isTextExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = selectedText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        // 折叠状态下显示摘要（前50字符+省略号）
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedText.length > 50)
                                "${selectedText.take(50)}..."
                            else
                                selectedText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Prompt模板选择
            PromptTemplateSelector(
                selectedTemplateId = selectedTemplateId,
                onTemplateSelected = { templateId ->
                    selectedTemplateId = templateId
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 处理结果区域
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                when (val state = uiState) {
                is ProcessTextUiState.Idle -> {
                    // 空闲状态：显示提示和开始按钮
                    Text(
                        text = "点击下方按钮开始处理文本",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is ProcessTextUiState.Loading -> {
                    // 加载中：显示进度指示器
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                    Text(
                        text = "正在连接API...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                is ProcessTextUiState.Processing -> {
                    // 流式处理中：显示累积文本
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "处理结果：",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Markdown文本可滚动
                            MarkdownText(
                                content = state.accumulatedText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (state.newChunk.isNotEmpty()) {
                                Text(
                                    text = "刚刚收到: ${state.newChunk}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            // 复制按钮（处理中状态）
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                val context = LocalContext.current
                                Button(
                                    onClick = {
                                        val clipboardManager = context.getSystemService(ClipboardManager::class.java)
                                        val clipData = ClipData.newPlainText("AI处理结果", state.accumulatedText)
                                        clipboardManager.setPrimaryClip(clipData)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Text("复制当前结果")
                                }
                            }
                        }
                    }
                }

                is ProcessTextUiState.Success -> {
                    // 处理成功：显示完整结果
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "处理完成：",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Markdown文本可滚动
                            MarkdownText(
                                content = state.fullText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // 复制按钮
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                val context = LocalContext.current
                                Button(
                                    onClick = {
                                        val clipboardManager = context.getSystemService(ClipboardManager::class.java)
                                        val clipData = ClipData.newPlainText("AI处理结果", state.fullText)
                                        clipboardManager.setPrimaryClip(clipData)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Text("复制结果")
                                }
                            }
                        }
                    }
                }

                is ProcessTextUiState.Error -> {
                    // 错误状态：显示错误信息
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "错误：",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 按钮区域
            when (val state = uiState) {
                is ProcessTextUiState.Idle -> {
                    // 空闲状态：显示处理文本按钮
                    Button(
                        onClick = {
                            viewModel.onEvent(
                                ProcessTextEvent.ProcessText(
                                    selectedText = selectedText,
                                    templateId = if (selectedTemplateId != 0L) selectedTemplateId else null
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("处理文本")
                    }
                }

                is ProcessTextUiState.Loading,
                is ProcessTextUiState.Processing -> {
                    // 处理中：显示取消按钮
                    OutlinedButton(
                        onClick = {
                            viewModel.onEvent(ProcessTextEvent.Cancel)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("取消处理")
                    }
                }

                is ProcessTextUiState.Success -> {
                    // 成功状态：显示重新处理按钮
                    Button(
                        onClick = {
                            viewModel.onEvent(
                                ProcessTextEvent.ProcessText(
                                    selectedText = selectedText,
                                    templateId = if (selectedTemplateId != 0L) selectedTemplateId else null
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("重新处理")
                    }
                }

                is ProcessTextUiState.Error -> {
                    // 错误状态：显示重试和清除错误按钮
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.onEvent(
                                    ProcessTextEvent.ProcessText(
                                        selectedText = selectedText,
                                        templateId = if (selectedTemplateId != 0L) selectedTemplateId else null
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("重试")
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.onEvent(ProcessTextEvent.ClearError)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("清除错误")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 关闭按钮
            OutlinedButton(
                onClick = { activity.finish() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("关闭")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun PromptTemplateSelector(
    selectedTemplateId: Long,
    onTemplateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appContainer = AITextSelectionAssistantApplication.getAppContainer(context)
    val promptTemplateRepository = appContainer.promptTemplateRepository

    // 模板列表状态
    val scope = rememberCoroutineScope()
    val templates by produceState(
        initialValue = emptyList(),
        key1 = promptTemplateRepository
    ) {
        // 在后台协程中收集模板列表
        scope.launch {
            promptTemplateRepository.getAllTemplates().collectLatest { templatesList ->
                value = templatesList
            }
        }
    }

    // 下拉菜单展开状态
    var expanded by remember { mutableStateOf(false) }

    // 如果没有模板，显示提示
    if (templates.isEmpty()) {
        Text(
            text = "暂无模板，请先添加Prompt模板",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier.padding(vertical = 8.dp)
        )
        return
    }

    // 选中的模板
    val selectedTemplate = templates.find { it.id == selectedTemplateId } ?: templates.firstOrNull()
    val selectedTemplateTitle = selectedTemplate?.title ?: "选择模板"

    Column(modifier = modifier) {
        Text(
            text = "选择Prompt模板：",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        // 下拉选择框
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = selectedTemplateTitle,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                templates.forEach { template ->
                    DropdownMenuItem(
                        text = { Text(template.title) },
                        onClick = {
                            onTemplateSelected(template.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}