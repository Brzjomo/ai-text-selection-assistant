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
    private var isEnabledByPref = true

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

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val source = event.source ?: return

            // 检查点击的元素是否是“复制”按钮
            // 这里使用简单的关键词匹配，涵盖中文和英文
            val text = source.text?.toString() ?: ""
            val contentDescription = source.contentDescription?.toString() ?: ""

            val isCopyAction = text.contains("复制", ignoreCase = true) ||
                    text.equals("Copy", ignoreCase = true) ||
                    contentDescription.contains("复制", ignoreCase = true) ||
                    contentDescription.equals("Copy", ignoreCase = true)

            if (isCopyAction) {
                // 延迟一点时间，等待系统将文本写入剪贴板
                serviceScope.launch {
                    delay(200)
                    checkClipboardAndLaunch()
                }
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