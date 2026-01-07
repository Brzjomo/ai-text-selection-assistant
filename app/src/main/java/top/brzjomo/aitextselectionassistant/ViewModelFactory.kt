package top.brzjomo.aitextselectionassistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import top.brzjomo.aitextselectionassistant.ui.main.ApiConfigViewModel
import top.brzjomo.aitextselectionassistant.ui.main.PromptViewModel

class ViewModelFactory(private val appContainer: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ApiConfigViewModel::class.java) -> {
                ApiConfigViewModel(appContainer.userPreferences) as T
            }
            modelClass.isAssignableFrom(PromptViewModel::class.java) -> {
                PromptViewModel(appContainer.promptTemplateRepository) as T
            }
            else -> {
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}