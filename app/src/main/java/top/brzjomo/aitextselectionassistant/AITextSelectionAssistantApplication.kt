package top.brzjomo.aitextselectionassistant

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import top.brzjomo.aitextselectionassistant.data.local.PromptTemplate
import top.brzjomo.aitextselectionassistant.AppContainer

class AITextSelectionAssistantApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        setupPresetTemplates()
    }

    private fun setupPresetTemplates() {
        applicationScope.launch {
            val repository = appContainer.promptTemplateRepository
            val existingTemplates = repository.getAllTemplates().firstOrNull() ?: emptyList()

            if (existingTemplates.isEmpty()) {
                val presetTemplates = listOf(
                    PromptTemplate(
                        title = "翻译成英文",
                        content = "将以下文本翻译成英文：{{text}}",
                        description = "将选中文本翻译为英文"
                    ),
                    PromptTemplate(
                        title = "翻译成中文",
                        content = "将以下文本翻译成中文：{{text}}",
                        description = "将选中文本翻译为中文"
                    ),
                    PromptTemplate(
                        title = "总结摘要",
                        content = "用简洁的语言总结以下内容：{{text}}",
                        description = "生成文本摘要"
                    ),
                    PromptTemplate(
                        title = "解释代码",
                        content = "解释以下代码的功能和逻辑：{{text}}",
                        description = "解释代码功能"
                    ),
                    PromptTemplate(
                        title = "润色文本",
                        content = "将以下文本润色为专业的商务邮件：{{text}}",
                        description = "文本润色和优化"
                    )
                )

                presetTemplates.forEach { template ->
                    repository.insertTemplate(template)
                }
            }
        }
    }

    companion object {
        @Suppress("DEPRECATION")
        fun getAppContainer(context: Context): AppContainer {
            return (context.applicationContext as AITextSelectionAssistantApplication).appContainer
        }
    }
}