package top.brzjomo.aitextselectionassistant.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.Material3RichText

/**
 * Markdown渲染组件，用于显示Markdown格式的文本
 */
@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier
    // 注意：onLinkClicked 在此库中通常由 LocalUriHandler 自动处理（调用系统浏览器），
    // 如果不需要拦截链接跳转，可以省略该参数，让库自动处理。
) {
    // 1. 使用 Material3RichText 作为容器
    // 它会自动读取当前的 MaterialTheme 颜色和排版，不需要手动设置 colorScheme.onSurface
    Material3RichText(
        modifier = modifier.fillMaxWidth()
    ) {
        // 2. 在容器内部调用 Markdown 解析内容
        Markdown(content = content)
    }
}

/**
 * 简化的Markdown文本组件，适合普通文本显示
 */
@Composable
fun SimpleMarkdownText(
    content: String,
    modifier: Modifier = Modifier
) {
    MarkdownText(
        content = content,
        modifier = modifier
    )
}