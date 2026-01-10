package top.brzjomo.aitextselectionassistant.service

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import top.brzjomo.aitextselectionassistant.AITextSelectionAssistantApplication
import top.brzjomo.aitextselectionassistant.MainActivity
import top.brzjomo.aitextselectionassistant.R
import top.brzjomo.aitextselectionassistant.ui.process.ProcessTextActivity

class ClipboardMonitorService : AccessibilityService(), ClipboardManager.OnPrimaryClipChangedListener {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isEnabledByPref = false
    private var clipboardManager: ClipboardManager? = null

    private var lastProcessedText: String? = null
    private var pollingJob: Job? = null

    companion object {
        private const val CHANNEL_ID = "ClipboardMonitorServiceChannel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        // 1. 立即启动前台服务（核心保活逻辑）
        startForegroundService()

        // 2. 检查悬浮窗权限
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请授予悬浮窗权限以使用自动弹窗功能", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener(this)

        serviceScope.launch {
            val userPreferences = (applicationContext as AITextSelectionAssistantApplication).appContainer.userPreferences
            userPreferences.appConfigFlow.collect { config ->
                isEnabledByPref = config.enableAutoClipboard
            }
        }
    }

    private fun startForegroundService() {
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI 划词助手正在运行")
            .setContentText("服务已启动，正在监听剪贴板")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 14 (SDK 34) 推荐在代码中也指定类型
                // 如果你的 compileSdk 是 36，可以直接使用 ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                // 对应的值是 32 (android.content.pm.ServiceInfo)
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "AI 助手后台服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保证应用在后台能监听到复制操作"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onPrimaryClipChanged() {
        if (isEnabledByPref) {
            checkClipboardAndLaunch()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isEnabledByPref || event == null) return
        if (event.packageName == packageName) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                if (isPotentialCopyAction(event)) {
                    startClipboardWatchdog()
                }
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                startClipboardWatchdog()
            }
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                val text = event.text.joinToString("")
                if (text.contains("复制") || text.contains("Copy")) {
                    checkClipboardAndLaunch()
                }
            }
        }
    }

    private fun startClipboardWatchdog() {
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            repeat(10) {
                if (checkClipboardAndLaunch()) return@launch
                delay(500)
            }
        }
    }

    private fun isPotentialCopyAction(event: AccessibilityEvent): Boolean {
        val source = event.source ?: return false
        val text = source.text?.toString() ?: ""
        val contentDescription = source.contentDescription?.toString() ?: ""
        val viewId = source.viewIdResourceName ?: ""

        val copyKeywords = listOf("复制", "Copy", "拷贝", "복사", "copy", "COPY")
        return copyKeywords.any { keyword ->
            text.contains(keyword, ignoreCase = true) ||
                    contentDescription.contains(keyword, ignoreCase = true) ||
                    viewId.contains("copy", ignoreCase = true)
        }
    }

    private fun checkClipboardAndLaunch(): Boolean {
        val cm = clipboardManager ?: return false
        if (!cm.hasPrimaryClip()) return false

        val clipData = cm.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val text = clipData.getItemAt(0).text?.toString()
            if (!text.isNullOrBlank() && text != lastProcessedText) {
                lastProcessedText = text
                launchProcessTextActivity(text)
                return true
            }
        }
        return false
    }

    private fun launchProcessTextActivity(text: String) {
        try {
            if (!Settings.canDrawOverlays(this)) return

            val intent = Intent(this, ProcessTextActivity::class.java).apply {
                action = Intent.ACTION_PROCESS_TEXT
                type = "text/plain"
                putExtra(Intent.EXTRA_PROCESS_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // 关键：不要使用 CLEAR_TASK，否则可能导致闪退或状态丢失
                // 但为了确保 Activity 收到 onNewIntent，singleInstance 已经足够
                addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果启动 Activity 失败（如被后台拦截），尝试发一个通知提示用户
            showLaunchFailedNotification(text)
        }
    }

    private fun showLaunchFailedNotification(text: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, ProcessTextActivity::class.java).apply {
            action = Intent.ACTION_PROCESS_TEXT
            type = "text/plain"
            putExtra(Intent.EXTRA_PROCESS_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("文本已复制，点击处理")
            .setContentText(text.take(20) + "...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        clipboardManager?.removePrimaryClipChangedListener(this)
        serviceScope.cancel()
    }

    override fun onInterrupt() {}
}