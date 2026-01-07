package top.brzjomo.aitextselectionassistant.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import top.brzjomo.aitextselectionassistant.AITextSelectionAssistantApplication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptEditScreen(
    templateId: Long? = null,
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
    val editState by viewModel.editState.collectAsState()

    LaunchedEffect(templateId) {
        if (templateId != null && templateId > 0) {
            // 这里应该加载特定模板，但当前ViewModel不支持
            // 暂时依赖外部调用startEditTemplate
        }
    }

    if (editState.isEditing) {
        EditTemplateForm(
            template = editState.template,
            onTemplateChange = { updatedTemplate ->
                viewModel.updateEditTemplate(updatedTemplate)
            },
            onSave = {
                viewModel.saveTemplate()
                onBack()
            },
            onCancel = {
                viewModel.cancelEdit()
                onBack()
            }
        )
    } else {
        onBack()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTemplateForm(
    template: top.brzjomo.aitextselectionassistant.data.local.PromptTemplate,
    onTemplateChange: (top.brzjomo.aitextselectionassistant.data.local.PromptTemplate) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(template.title) }
    var content by remember { mutableStateOf(template.content) }
    var description by remember { mutableStateOf(template.description) }

    LaunchedEffect(template) {
        title = template.title
        content = template.content
        description = template.description
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (template.id == 0L) "添加模板" else "编辑模板") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSave,
                        enabled = title.isNotBlank() && content.isNotBlank()
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
                value = title,
                onValueChange = {
                    title = it
                    onTemplateChange(template.copy(title = it))
                },
                label = @Composable { Text("模板标题") },
                placeholder = @Composable { Text("例如：翻译成英文") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    onTemplateChange(template.copy(description = it))
                },
                label = @Composable { Text("描述（可选）") },
                placeholder = @Composable { Text("简要描述模板用途") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = content,
                onValueChange = {
                    content = it
                    onTemplateChange(template.copy(content = it))
                },
                label = @Composable { Text("模板内容") },
                placeholder = @Composable { Text("使用 {{text}} 作为文本占位符") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "模板变量说明",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "在模板内容中使用 {{text}} 表示用户选中的文本。\n例如：\"将以下文本翻译成英文：{{text}}\"",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "系统会自动将 {{text}} 替换为实际选中的文本。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Button(
                onClick = onSave,
                enabled = title.isNotBlank() && content.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存模板")
            }
        }
    }
}