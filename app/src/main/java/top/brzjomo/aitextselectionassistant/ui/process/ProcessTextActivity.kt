package top.brzjomo.aitextselectionassistant.ui.process

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.brzjomo.aitextselectionassistant.AITextSelectionAssistantApplication
import top.brzjomo.aitextselectionassistant.ViewModelFactory
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
            AITextSelectionAssistantTheme {
                ProcessTextScreen(
                    selectedText = selectedText,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun ProcessTextScreen(
    selectedText: String,
    viewModel: ProcessTextViewModel
) {
    val context = LocalContext.current
    val activity = context as Activity

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 标题
            Text(
                text = "AI 划词助手",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 选中的文本显示
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "选中的文本：",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 处理结果区域
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
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "处理结果：",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            MarkdownText(
                                content = state.accumulatedText,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (state.newChunk.isNotEmpty()) {
                                Text(
                                    text = "刚刚收到: ${state.newChunk}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
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
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "处理完成：",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            MarkdownText(
                                content = state.fullText,
                                modifier = Modifier.fillMaxWidth()
                            )
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

            Spacer(modifier = Modifier.height(24.dp))

            // 按钮区域
            when (val state = uiState) {
                is ProcessTextUiState.Idle -> {
                    // 空闲状态：显示处理文本按钮
                    Button(
                        onClick = {
                            viewModel.onEvent(ProcessTextEvent.ProcessText(selectedText))
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
                            viewModel.onEvent(ProcessTextEvent.ProcessText(selectedText))
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
                                viewModel.onEvent(ProcessTextEvent.ProcessText(selectedText))
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