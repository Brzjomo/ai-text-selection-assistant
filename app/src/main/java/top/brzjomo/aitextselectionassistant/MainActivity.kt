package top.brzjomo.aitextselectionassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import top.brzjomo.aitextselectionassistant.ui.main.MainScreen
import top.brzjomo.aitextselectionassistant.ui.theme.AITextSelectionAssistantTheme
import top.brzjomo.aitextselectionassistant.AppContainer
import top.brzjomo.aitextselectionassistant.LocalAppContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 创建 AppContainer
        val appContainer = AppContainer(this)

        setContent {
            CompositionLocalProvider(
                LocalAppContainer provides appContainer
            ) {
                AITextSelectionAssistantTheme {
                    MainScreen()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    AITextSelectionAssistantTheme {
        MainScreen()
    }
}