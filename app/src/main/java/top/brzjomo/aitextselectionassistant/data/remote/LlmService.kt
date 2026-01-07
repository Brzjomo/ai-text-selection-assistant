package top.brzjomo.aitextselectionassistant.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming
import top.brzjomo.aitextselectionassistant.data.remote.model.ChatRequest

interface LlmService {
    @POST("chat/completions")
    @Headers("Content-Type: application/json")
    @Streaming
    suspend fun chatCompletion(
        @Body request: ChatRequest
    ): Response<ResponseBody>
}