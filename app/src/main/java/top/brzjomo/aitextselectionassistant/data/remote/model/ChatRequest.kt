package top.brzjomo.aitextselectionassistant.data.remote.model

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("model")
    val model: String,
    @SerializedName("messages")
    val messages: List<ChatMessage>,
    @SerializedName("stream")
    val stream: Boolean = true,
    @SerializedName("max_tokens")
    val maxTokens: Int? = null,
    @SerializedName("temperature")
    val temperature: Double? = null,
    @SerializedName("top_p")
    val topP: Double? = null
)