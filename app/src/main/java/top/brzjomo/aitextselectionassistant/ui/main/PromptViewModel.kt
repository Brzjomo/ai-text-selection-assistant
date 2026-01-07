package top.brzjomo.aitextselectionassistant.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import top.brzjomo.aitextselectionassistant.AITextSelectionAssistantApplication
import top.brzjomo.aitextselectionassistant.data.local.PromptTemplate
import top.brzjomo.aitextselectionassistant.data.repository.PromptTemplateRepository

sealed interface PromptUiState {
    data class Success(val templates: List<PromptTemplate>) : PromptUiState
    object Loading : PromptUiState
    data class Error(val message: String) : PromptUiState
}

data class PromptEditState(
    val template: PromptTemplate = PromptTemplate(
        title = "",
        content = "",
        description = ""
    ),
    val isEditing: Boolean = false
)

class PromptViewModel(
    context: Context
) : ViewModel() {
    private val repository = AITextSelectionAssistantApplication.getAppContainer(context).promptTemplateRepository

    private val _uiState = MutableStateFlow<PromptUiState>(PromptUiState.Loading)
    val uiState: StateFlow<PromptUiState> = _uiState.asStateFlow()

    private val _editState = MutableStateFlow(PromptEditState())
    val editState: StateFlow<PromptEditState> = _editState.asStateFlow()

    init {
        loadTemplates()
    }

    fun loadTemplates() {
        viewModelScope.launch {
            try {
                repository.getAllTemplates().collect { templates ->
                    _uiState.value = PromptUiState.Success(templates)
                }
            } catch (e: Exception) {
                _uiState.value = PromptUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    fun startAddTemplate() {
        _editState.value = PromptEditState(
            template = PromptTemplate(
                title = "",
                content = "",
                description = ""
            ),
            isEditing = true
        )
    }

    fun startEditTemplate(template: PromptTemplate) {
        _editState.value = PromptEditState(
            template = template.copy(),
            isEditing = true
        )
    }

    fun updateEditTemplate(template: PromptTemplate) {
        _editState.update { currentState ->
            currentState.copy(template = template)
        }
    }

    fun saveTemplate() {
        viewModelScope.launch {
            val currentTemplate = _editState.value.template
            try {
                if (currentTemplate.id == 0L) {
                    repository.insertTemplate(currentTemplate)
                } else {
                    repository.updateTemplate(currentTemplate.copy(updatedAt = System.currentTimeMillis()))
                }
                _editState.value = PromptEditState()
                loadTemplates()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun deleteTemplate(template: PromptTemplate) {
        viewModelScope.launch {
            try {
                repository.deleteTemplate(template)
                loadTemplates()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun cancelEdit() {
        _editState.value = PromptEditState()
    }
}