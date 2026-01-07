package top.brzjomo.aitextselectionassistant.data.remote.model

import com.google.gson.annotations.SerializedName

data class ChatStreamChunk(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("object")
    val objectType: String? = null,
    @SerializedName("created")
    val created: Long? = null,
    @SerializedName("model")
    val model: String? = null,
    @SerializedName("choices")
    val choices: List<Choice>? = emptyList()
) {
    data class Choice(
        @SerializedName("index")
        val index: Int? = null,
        @SerializedName("delta")
        val delta: Delta? = null,
        @SerializedName("finish_reason")
        val finishReason: String? = null
    )

    data class Delta(
        @SerializedName("content")
        val content: String? = null,
        @SerializedName("role")
        val role: String? = null
    )
}