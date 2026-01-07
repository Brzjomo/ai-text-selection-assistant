package top.brzjomo.aitextselectionassistant.data.remote.model

import com.google.gson.annotations.SerializedName

data class ChatMessage(
    @SerializedName("role")
    val role: String, // "system", "user", "assistant"
    @SerializedName("content")
    val content: String
)