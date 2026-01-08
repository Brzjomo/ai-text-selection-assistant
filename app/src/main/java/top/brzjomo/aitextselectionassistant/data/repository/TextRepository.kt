package top.brzjomo.aitextselectionassistant.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody
import retrofit2.Response
import top.brzjomo.aitextselectionassistant.AppContainer
import top.brzjomo.aitextselectionassistant.data.local.ApiConfig
import top.brzjomo.aitextselectionassistant.data.local.ApiProvider
import top.brzjomo.aitextselectionassistant.data.local.ProviderType
import top.brzjomo.aitextselectionassistant.data.local.PromptTemplate
import top.brzjomo.aitextselectionassistant.data.remote.model.ChatMessage
import top.brzjomo.aitextselectionassistant.data.remote.model.ChatRequest
import java.io.IOException

class TextRepository(private val appContainer: AppContainer) {

    private val userPreferences = appContainer.userPreferences
    private val promptTemplateRepository = appContainer.promptTemplateRepository
    private val apiProviderRepository = appContainer.apiProviderRepository
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
        // 1. 获取API配置（优先使用新的服务商系统）
        val apiProvider = try {
            val defaultProvider = apiProviderRepository.getDefaultProvider()
            if (defaultProvider != null) {
                defaultProvider
            } else {
                val allProviders = apiProviderRepository.getAllProviders().firstOrNull()
                allProviders?.firstOrNull()
            }
        } catch (e: Exception) {
            null
        }

        val apiConfig = if (apiProvider != null) {
            // 使用新的服务商系统
            if (!apiProvider.isValid) {
                throw IllegalArgumentException("API服务商配置不完整，请检查Base URL、API密钥（如需要）和模型名称")
            }
            apiProvider.toApiConfig()
        } else {
            // 回退到旧的配置系统
            val oldConfig = userPreferences.apiConfigFlow.firstOrNull() ?: ApiConfig()
            if (!oldConfig.isValid) {
                throw IllegalArgumentException("API配置不完整，请先配置API密钥和Base URL")
            }
            oldConfig
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
        val llmService = if (apiProvider != null) {
            appContainer.createLlmService(apiProvider)
        } else {
            appContainer.createLlmService(apiConfig)
        }
        val response: Response<ResponseBody> = try {
            llmService.chatCompletion(request)
        } catch (e: IOException) {
            val errorMessage = if (apiProvider?.providerType == ProviderType.OLLAMA) {
                "Ollama连接失败: ${e.message}\n\n请检查:\n1. Ollama服务是否正在运行\n2. IP地址和端口是否正确\n3. 网络是否连通\n4. 防火墙是否允许连接"
            } else {
                "网络连接失败: ${e.message}"
            }
            throw IOException(errorMessage, e)
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