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
                        title = "翻译成中文",
                        content = "将以下文本翻译为简体中文，注意只需要输出翻译后的结果，不要额外解释：{{text}}",
                        description = "将选中文本翻译为中文"
                    ),
                    PromptTemplate(
                        title = "总结摘要",
                        content = "用简洁的语言总结以下内容：{{text}}",
                        description = "生成文本摘要"
                    ),
                    PromptTemplate(
                        title = "润色文本",
                        content = "将以下文本润色为专业的商务邮件：{{text}}",
                        description = "文本润色和优化"
                    ),
                    PromptTemplate(
                        title = "单词完整解析",
                        content = """
        请对英语单词"{{text}}"进行完整详细的解析，按以下结构化格式输出（注意换行）：
        
        🔤 单词：{{text}}
        
        📢 发音音标：
          • 英式音标：/[英式IPA音标]/
          • 美式音标：/[美式IPA音标]/
        
        📖 中文释义：
          [按词性分类列出主要中文意思，每个意思单独一行]
        
        🏷️ 词性：
          [标注所有可能的词性，如：n. (名词), v. (动词), adj. (形容词), adv. (副词)等]
        
        🔄 动词变形（如适用）：
          • 原型：[动词原形]
          • 第三人称单数：[加s/es形式]
          • 现在分词：[-ing形式]
          • 过去式：[过去式形式]
          • 过去分词：[过去分词形式]
        
        🌳 派生词：
          • [相关名词形式]
          • [相关形容词形式]
          • [相关副词形式]
          • [反义词]
          • [同义词]
        
        📚 词源（Etymology）：
          [简要说明单词的来源和历史演变]
        
        🔗 常用短语搭配：
          1. [短语1] - [中文解释]
          2. [短语2] - [中文解释]
          3. [短语3] - [中文解释]
        
        📝 实用例句（带中文翻译）：
          1. [英文例句1]
             [中文翻译1]
          2. [英文例句2]
             [中文翻译2]
          3. [英文例句3]
             [中文翻译3]
        
        💡 使用提示：
          [简要的使用注意事项或常见错误]
        
        注意：如果某些项目不适用于该单词（如非动词没有分词形式），请标注"N/A"。
        保持格式清晰，不要添加额外解释或说明性文字。
    """.trimIndent(),
                        description = "获取英语单词的完整解析，包括音标、词性、变形、词源、派生词、短语和例句"
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