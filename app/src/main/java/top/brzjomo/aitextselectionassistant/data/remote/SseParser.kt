package top.brzjomo.aitextselectionassistant.data.remote

import android.util.Log
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
        private const val TAG = "SseParser"
        private const val DATA_PREFIX = "data: "
        private const val DONE_SIGNAL = "[DONE]"
    }

    fun parseStream(inputStream: InputStream): Flow<String> = flow {
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            var line: String?
            var lineNumber = 0
            while (reader.readLine().also { line = it } != null) {
                lineNumber++
                val currentLine = line ?: break
                if (currentLine.isBlank()) {
                    continue // 跳过空行
                }
                if (currentLine.startsWith(DATA_PREFIX)) {
                    val data = currentLine.substring(DATA_PREFIX.length)
                    if (data == DONE_SIGNAL) {
                        Log.d(TAG, "收到[DONE]信号，流式传输结束")
                        return@flow
                    }
                    try {
                        Log.d(TAG, "解析第${lineNumber}行数据: $data")
                        val chunk = gson.fromJson(data, ChatStreamChunk::class.java)
                        val content = chunk.choices?.firstOrNull()?.delta?.content
                        if (!content.isNullOrEmpty()) {
                            Log.d(TAG, "发射内容: '$content'")
                            emit(content)
                        } else {
                            Log.d(TAG, "内容为空，可能只是role或元数据")
                        }
                    } catch (e: JsonSyntaxException) {
                        Log.w(TAG, "JSON解析错误第${lineNumber}行: $currentLine", e)
                        // 忽略JSON解析错误，继续处理下一行
                    } catch (e: Exception) {
                        Log.e(TAG, "解析第${lineNumber}行时发生错误: $currentLine", e)
                        // 其他异常处理
                    }
                } else {
                    Log.d(TAG, "跳过非data前缀行[$lineNumber]: $currentLine")
                }
            }
            Log.d(TAG, "流式传输结束，共处理${lineNumber}行")
        }
    }
}

