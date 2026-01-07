package top.brzjomo.aitextselectionassistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.content.Context
import top.brzjomo.aitextselectionassistant.ui.main.ApiConfigViewModel
import top.brzjomo.aitextselectionassistant.ui.main.PromptViewModel
import top.brzjomo.aitextselectionassistant.ui.process.ProcessTextViewModel
import top.brzjomo.aitextselectionassistant.data.repository.TextRepository
import top.brzjomo.aitextselectionassistant.AppContainer

class ViewModelFactory(
    private val appContainer: AppContainer,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ApiConfigViewModel::class.java) -> {
                ApiConfigViewModel(context) as T
            }
            modelClass.isAssignableFrom(PromptViewModel::class.java) -> {
                PromptViewModel(context) as T
            }
            modelClass.isAssignableFrom(ProcessTextViewModel::class.java) -> {
                val textRepository = TextRepository(appContainer)
                ProcessTextViewModel(textRepository) as T
            }
            else -> {
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}