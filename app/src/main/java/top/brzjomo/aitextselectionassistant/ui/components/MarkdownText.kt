package top.brzjomo.aitextselectionassistant.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    // 标准Markdown会将单个换行符视为空格。
    // 为了在手机上正确显示类似"单词解析"这种每行独立的文本，
    // 我们将单个 "\n" 替换为 "  \n" (两个空格+换行)，强制触发Markdown的硬换行。
    val formattedContent = remember(content) {
        content.replace("\n", "  \n")
    }
    // 使用 Material3RichText 作为容器
    // 它会自动读取当前的 MaterialTheme 颜色和排版，不需要手动设置 colorScheme.onSurface
    Material3RichText(
        modifier = modifier.fillMaxWidth()
    ) {
        // 在容器内部调用 Markdown 解析内容
        Markdown(content = formattedContent)
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