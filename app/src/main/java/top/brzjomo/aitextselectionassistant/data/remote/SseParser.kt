package top.brzjomo.aitextselectionassistant.data.remote

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import top.brzjomo.aitextselectionassistant.data.remote.model.ChatStreamChunk
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class SseParser(private val gson: Gson = Gson()) {
    companion object {
        private const val DATA_PREFIX = "data: "
        private const val DONE_SIGNAL = "[DONE]"
    }

    fun parseStream(inputStream: InputStream): Flow<String> = flow {
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            reader.lineSequence().forEach { line ->
                if (line.startsWith(DATA_PREFIX)) {
                    val data = line.substring(DATA_PREFIX.length)
                    if (data == DONE_SIGNAL) {
                        return@flow
                    }
                    try {
                        val chunk = gson.fromJson(data, ChatStreamChunk::class.java)
                        val content = chunk.choices?.firstOrNull()?.delta?.content
                        if (!content.isNullOrEmpty()) {
                            emit(content)
                        }
                    } catch (e: JsonSyntaxException) {
                        // 忽略解析错误，继续处理下一行
                    } catch (e: Exception) {
                        // 其他异常处理
                    }
                }
            }
        }
    }
}

