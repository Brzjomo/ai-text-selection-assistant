package top.brzjomo.aitextselectionassistant.ui.main

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import top.brzjomo.aitextselectionassistant.data.local.PromptTemplate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PromptListScreen(
    onEditTemplate: (Long) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel = viewModel<PromptViewModel>(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(PromptViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return PromptViewModel(context) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    // 本地维护一个可变列表以实现实时 UI 响应
    val templates = remember { mutableStateListOf<PromptTemplate>() }

    // 监听 ViewModel 数据变化并同步到本地列表
    LaunchedEffect(uiState) {
        if (uiState is PromptUiState.Success) {
            val newTemplates = (uiState as PromptUiState.Success).templates
            // 简单比对一下 ID 列表，防止不必要的刷新
            if (templates.map { it.id } != newTemplates.map { it.id }) {
                templates.clear()
                templates.addAll(newTemplates)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadTemplates()
    }

    // 震动反馈辅助函数
    fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prompt 模板管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEditTemplate(0L) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加模板")
            }
        }
    ) { paddingValues ->
        if (templates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无模板，点击右下角按钮添加")
            }
        } else {
            // 记录当前正在拖拽的 Item 的 ID 和垂直偏移量
            var draggingItemId by remember { mutableStateOf<Long?>(null) }
            var draggingItemOffset by remember { mutableFloatStateOf(0f) }
            // 记录每个 Item 的高度，用于计算交换阈值
            val itemHeights = remember { mutableStateMapOf<Long, Int>() }
            val scope = rememberCoroutineScope()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                itemsIndexed(
                    items = templates,
                    // 关键：必须指定 key，否则动画无法正常工作
                    key = { _, item -> item.id }
                ) { index, template ->
                    val isDragging = draggingItemId == template.id

                    // 拖拽时的视觉效果
                    val elevation by animateFloatAsState(if (isDragging) 8f else 0f, label = "elevation")
                    val scale by animateFloatAsState(if (isDragging) 1.05f else 1f, label = "scale")

                    // 构建 Modifier 链
                    // 如果当前 Item 正在被拖拽，就不应用 animateItemPlacement()，完全由 draggingItemOffset 控制位置
                    // 如果没有被拖拽（是其他让位的 Item），则应用动画实现平滑移动
                    val modifier = Modifier
                        .then(if (isDragging) Modifier else Modifier.animateItemPlacement())
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragging) draggingItemOffset else 0f
                            scaleX = scale
                            scaleY = scale
                            shadowElevation = elevation
                            // 可选：拖拽时稍微降低不透明度，增强视觉反馈
                            alpha = if (isDragging) 0.9f else 1f
                        }
                        .onSizeChanged { size ->
                            itemHeights[template.id] = size.height
                        }

                    Box(modifier = modifier) {
                        PromptTemplateCard(
                            template = template,
                            onEdit = { onEditTemplate(template.id) },
                            onDelete = { viewModel.deleteTemplate(template) },
                            // 将拖拽监听放在卡片容器上
                            modifier = Modifier.pointerInput(template.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingItemId = template.id
                                        draggingItemOffset = 0f
                                        vibrate()
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        draggingItemOffset += dragAmount.y

                                        val currentId = draggingItemId ?: return@detectDragGesturesAfterLongPress
                                        val currentIndex = templates.indexOfFirst { it.id == currentId }
                                        if (currentIndex == -1) return@detectDragGesturesAfterLongPress

                                        val currentHeight = itemHeights[currentId] ?: 0
                                        val swapThreshold = currentHeight / 2

                                        // 向下拖动
                                        if (draggingItemOffset > swapThreshold && currentIndex < templates.lastIndex) {
                                            val targetIndex = currentIndex + 1
                                            // 交换数据
                                            templates.apply {
                                                add(targetIndex, removeAt(currentIndex))
                                            }
                                            // 修正偏移量：Item 位置变了，手动减去高度差以保持视觉位置不变
                                            val targetHeight = itemHeights[templates[currentIndex].id] ?: currentHeight
                                            draggingItemOffset -= (targetHeight + 8.dp.toPx())
                                            vibrate()
                                        }
                                        // 向上拖动
                                        else if (draggingItemOffset < -swapThreshold && currentIndex > 0) {
                                            val targetIndex = currentIndex - 1
                                            // 交换数据
                                            templates.apply {
                                                add(targetIndex, removeAt(currentIndex))
                                            }
                                            // 修正偏移量
                                            val targetHeight = itemHeights[templates[currentIndex].id] ?: currentHeight
                                            draggingItemOffset += (targetHeight + 8.dp.toPx())
                                            vibrate()
                                        }
                                    },
                                    onDragEnd = {
                                        draggingItemId = null
                                        draggingItemOffset = 0f
                                        scope.launch {
                                            viewModel.reorderTemplates(templates.toList())
                                            // 拖拽结束后，可能需要重新加载以确保一致性，
                                            // 但为了避免列表闪烁，通常这里不做全量刷新，除非有错误
                                        }
                                    },
                                    onDragCancel = {
                                        draggingItemId = null
                                        draggingItemOffset = 0f
                                        viewModel.loadTemplates()
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptTemplateCard(
    template: PromptTemplate,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onEdit
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            }
            if (template.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = template.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}