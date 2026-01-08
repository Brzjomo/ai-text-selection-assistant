package top.brzjomo.aitextselectionassistant.ui.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import top.brzjomo.aitextselectionassistant.AITextSelectionAssistantApplication
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
                    return PromptViewModel(context) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val templates = when (val state = uiState) {
        is PromptUiState.Success -> state.templates
        PromptUiState.Loading -> emptyList()
        is PromptUiState.Error -> emptyList()
    }

    // Local reorderable list state
    val scope = rememberCoroutineScope()
    val reorderableTemplates = remember(templates) { templates.toMutableStateList() }
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var startDragIndex by remember { mutableIntStateOf(-1) }
    var totalDragOffset by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadTemplates()
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                itemsIndexed(reorderableTemplates) { index, template ->
                    val density = LocalDensity.current
                    val modifier = Modifier.pointerInput(template.id) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                startDragIndex = index
                                draggedIndex = index
                                isDragging = true
                                totalDragOffset = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDragOffset += dragAmount.y

                                // Real-time reordering during drag
                                with(density) {
                                    val itemHeight = 80.dp.toPx() // Reduced height for better sensitivity
                                    // Calculate target position based on total drag offset from start
                                    val delta = (totalDragOffset / itemHeight).roundToInt()
                                    val targetIndex = (startDragIndex + delta).coerceIn(0, reorderableTemplates.lastIndex)

                                    if (targetIndex != draggedIndex) {
                                        // Move the dragged item to new position
                                        val list = reorderableTemplates
                                        val item = list.removeAt(draggedIndex)
                                        // Adjust target index after removal
                                        val adjustedTargetIndex = if (targetIndex > draggedIndex) targetIndex - 1 else targetIndex
                                        list.add(adjustedTargetIndex, item)
                                        // Update dragged index to new position (actual position)
                                        draggedIndex = adjustedTargetIndex
                                        // Adjust total drag offset to match new position
                                        totalDragOffset = (adjustedTargetIndex - startDragIndex) * itemHeight
                                    }
                                }
                            },
                            onDragEnd = {
                                isDragging = false
                                // Final update to ViewModel after drag ends
                                scope.launch {
                                    viewModel.reorderTemplates(reorderableTemplates.toList())
                                    viewModel.loadTemplates()
                                }
                                draggedIndex = -1
                                startDragIndex = -1
                                totalDragOffset = 0f
                            },
                            onDragCancel = {
                                isDragging = false
                                draggedIndex = -1
                                startDragIndex = -1
                                totalDragOffset = 0f
                            }
                        )
                    }
                    PromptTemplateCard(
                        template = template,
                        onEdit = { onEditTemplate(template.id) },
                        onDelete = { viewModel.deleteTemplate(template) },
                        modifier = modifier,
                        isBeingDragged = index == draggedIndex && isDragging
                    )
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
    modifier: Modifier = Modifier,
    isBeingDragged: Boolean = false
) {
    val cardModifier = if (isBeingDragged) {
        modifier
            .fillMaxWidth()
            .graphicsLayer {
                shadowElevation = 16.dp.toPx()
                alpha = 0.9f
                scaleX = 1.02f
                scaleY = 1.02f
                translationY = -8.dp.toPx()
            }
    } else {
        modifier.fillMaxWidth()
    }

    Card(
        modifier = cardModifier,
        onClick = onEdit
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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