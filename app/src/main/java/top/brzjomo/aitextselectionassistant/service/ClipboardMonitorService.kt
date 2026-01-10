package top.brzjomo.aitextselectionassistant.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import top.brzjomo.aitextselectionassistant.AITextSelectionAssistantApplication
import top.brzjomo.aitextselectionassistant.ui.process.ProcessTextActivity

class ClipboardMonitorService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isEnabledByPref = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        // 监听配置变化
        serviceScope.launch {
            val userPreferences = (applicationContext as AITextSelectionAssistantApplication).appContainer.userPreferences
            userPreferences.appConfigFlow.collect { config ->
                isEnabledByPref = config.enableAutoClipboard
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isEnabledByPref || event == null) return

        // 忽略自己应用的事件，防止死循环
        if (event.packageName == packageName) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                handleViewClicked(event)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                // 文本选择变化事件，可能是用户选择了文本
                // 延迟检查剪贴板，用户可能在选择后立即复制
                serviceScope.launch {
                    delay(300) // 给用户时间执行复制操作
                    checkClipboardAndLaunch()
                }
            }
        }
    }

    private fun handleViewClicked(event: AccessibilityEvent) {
        val source = event.source ?: return

        // 检查点击的元素是否是“复制”按钮
        // 这里使用更全面的关键词匹配
        val text = source.text?.toString() ?: ""
        val contentDescription = source.contentDescription?.toString() ?: ""
        val className = source.className?.toString() ?: ""

        // 扩展关键词列表，包括更多可能的复制按钮文本
        val copyKeywords = listOf(
            "复制", "Copy", "拷贝", "複製", "コピー", "복사",
            "copy", "COPY", "복사하기", "Kopieren", "copier"
        )

        val isCopyAction = copyKeywords.any { keyword ->
            text.contains(keyword, ignoreCase = true) ||
                    contentDescription.contains(keyword, ignoreCase = true) ||
                    className.contains("copy", ignoreCase = true) ||
                    className.contains("clipboard", ignoreCase = true)
        }

        // 检查视图ID名称（如果有）
        val viewIdResourceName = try {
            source.viewIdResourceName
        } catch (e: Exception) {
            null
        }

        val isCopyById = viewIdResourceName?.let { idName ->
            idName.contains("copy", ignoreCase = true) ||
            idName.contains("clipboard", ignoreCase = true)
        } ?: false

        if (isCopyAction || isCopyById) {
            // 延迟一点时间，等待系统将文本写入剪贴板
            serviceScope.launch {
                delay(200)
                checkClipboardAndLaunch()
            }
        }
    }

    private fun checkClipboardAndLaunch() {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (!clipboardManager.hasPrimaryClip()) return

        val clipData = clipboardManager.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val text = clipData.getItemAt(0).text?.toString()

            if (!text.isNullOrBlank()) {
                launchProcessTextActivity(text)
            }
        }
    }

    private fun launchProcessTextActivity(text: String) {
        try {
            val intent = Intent(this, ProcessTextActivity::class.java).apply {
                action = Intent.ACTION_PROCESS_TEXT
                type = "text/plain"
                putExtra(Intent.EXTRA_PROCESS_TEXT, text)
                // 从 Service 启动 Activity 需要此 Flag
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // 如果需要，可以添加清除顶部的 Flag，确保每次都是新窗口
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInterrupt() {
        // 服务中断时的处理
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}