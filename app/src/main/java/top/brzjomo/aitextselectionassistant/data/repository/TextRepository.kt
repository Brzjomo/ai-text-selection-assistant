package top.brzjomo.aitextselectionassistant.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import retrofit2.Response
import top.brzjomo.aitextselectionassistant.AppContainer
import top.brzjomo.aitextselectionassistant.data.local.ApiConfig
import top.brzjomo.aitextselectionassistant.data.local.PromptTemplate
import top.brzjomo.aitextselectionassistant.data.remote.model.ChatMessage
import top.brzjomo.aitextselectionassistant.data.remote.model.ChatRequest
import java.io.IOException

class TextRepository(private val appContainer: AppContainer) {

    private val userPreferences = appContainer.userPreferences
    private val promptTemplateRepository = appContainer.promptTemplateRepository
    private val sseParser = appContainer.sseParser

    // 模板引擎：简单替换 {{text}} 占位符
    private fun renderTemplate(template: String, text: String): String {
        return template.replace("{{text}}", text)
    }

    /**
     * 处理文本：使用指定的模板ID处理用户选中的文本
     * @param selectedText 用户选中的文本
     * @param templateId 使用的模板ID，如果为null则使用第一个模板
     * @return 流式响应 Flow<String>
     */
    fun processText(
        selectedText: String,
        templateId: Long? = null
    ): Flow<String> = flow {
        // 1. 获取API配置
        val apiConfig = userPreferences.apiConfigFlow.firstOrNull() ?: ApiConfig()
        if (apiConfig.apiKey.isBlank() || apiConfig.baseUrl.isBlank()) {
            throw IllegalArgumentException("API配置不完整，请先配置API密钥和Base URL")
        }

        // 2. 获取模板
        val template = if (templateId != null) {
            promptTemplateRepository.getTemplateById(templateId)
        } else {
            // 使用第一个模板
            promptTemplateRepository.getAllTemplates().firstOrNull()?.firstOrNull()
        } ?: throw IllegalArgumentException("未找到可用的模板")

        // 3. 渲染模板
        val renderedContent = renderTemplate(template.content, selectedText)

        // 4. 构建请求
        val messages = listOf(
            ChatMessage(role = "user", content = renderedContent)
        )

        val request = ChatRequest(
            model = apiConfig.model,
            messages = messages,
            stream = apiConfig.enableStreaming,
            maxTokens = if (apiConfig.maxTokens > 0) apiConfig.maxTokens else null,
            temperature = apiConfig.temperature.takeIf { it > 0.0 }
        )

        // 5. 创建LlmService并发送请求
        val llmService = appContainer.createLlmService(apiConfig)
        val response: Response<ResponseBody> = try {
            llmService.chatCompletion(request)
        } catch (e: IOException) {
            throw IOException("网络连接失败: ${e.message}", e)
        } catch (e: Exception) {
            throw RuntimeException("API请求失败: ${e.message}", e)
        }

        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string() ?: "未知错误"
            throw IOException("API请求失败: ${response.code()} - $errorBody")
        }

        val responseBody = response.body() ?: throw IOException("响应体为空")

        // 6. 解析流式响应
        sseParser.parseStream(responseBody.byteStream()).collect { chunk ->
            emit(chunk)
        }
    }

}