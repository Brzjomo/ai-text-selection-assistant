package top.brzjomo.aitextselectionassistant.ui.process

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import top.brzjomo.aitextselectionassistant.data.repository.TextRepository
import java.io.IOException

sealed interface ProcessTextUiState {
    object Idle : ProcessTextUiState
    object Loading : ProcessTextUiState
    data class Processing(val accumulatedText: String, val newChunk: String) : ProcessTextUiState
    data class Success(val fullText: String) : ProcessTextUiState
    data class Error(val message: String) : ProcessTextUiState
}

sealed class ProcessTextEvent {
    data class ProcessText(val selectedText: String, val templateId: Long? = null) : ProcessTextEvent()
    object Cancel : ProcessTextEvent()
    object Retry : ProcessTextEvent()
    object ClearError : ProcessTextEvent()
}

class ProcessTextViewModel(
    private val textRepository: TextRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProcessTextUiState>(ProcessTextUiState.Idle)
    val uiState: StateFlow<ProcessTextUiState> = _uiState.asStateFlow()

    private var currentProcessingJob: kotlinx.coroutines.Job? = null

    fun onEvent(event: ProcessTextEvent) {
        when (event) {
            is ProcessTextEvent.ProcessText -> processText(event.selectedText, event.templateId)
            is ProcessTextEvent.Cancel -> cancelProcessing()
            is ProcessTextEvent.Retry -> retryProcessing()
            is ProcessTextEvent.ClearError -> clearError()
        }
    }

    private fun processText(selectedText: String, templateId: Long?) {
        // 取消当前正在进行的处理
        currentProcessingJob?.cancel()

        currentProcessingJob = viewModelScope.launch {
            _uiState.value = ProcessTextUiState.Loading

            try {
                var accumulatedText = ""

                textRepository.processText(selectedText, templateId).collect { chunk ->
                    accumulatedText += chunk
                    _uiState.value = ProcessTextUiState.Processing(
                        accumulatedText = accumulatedText,
                        newChunk = chunk
                    )
                }

                // 流式处理完成
                _uiState.value = ProcessTextUiState.Success(fullText = accumulatedText)
            } catch (e: IOException) {
                _uiState.value = ProcessTextUiState.Error(
                    message = "网络错误: ${e.message ?: "请检查网络连接和API配置"}"
                )
            } catch (e: IllegalArgumentException) {
                _uiState.value = ProcessTextUiState.Error(
                    message = "配置错误: ${e.message ?: "请检查API配置和模板设置"}"
                )
            } catch (e: Exception) {
                _uiState.value = ProcessTextUiState.Error(
                    message = "处理失败: ${e.message ?: "未知错误"}"
                )
            }
        }
    }

    private fun cancelProcessing() {
        currentProcessingJob?.cancel()
        _uiState.value = ProcessTextUiState.Idle
    }

    private fun retryProcessing() {
        val currentState = _uiState.value
        if (currentState is ProcessTextUiState.Error) {
            // 需要外部调用者提供要重试的文本和模板ID
            // 这里只是清除错误状态，实际重试需要外部触发
            _uiState.value = ProcessTextUiState.Idle
        }
    }

    private fun clearError() {
        if (_uiState.value is ProcessTextUiState.Error) {
            _uiState.value = ProcessTextUiState.Idle
        }
    }
}