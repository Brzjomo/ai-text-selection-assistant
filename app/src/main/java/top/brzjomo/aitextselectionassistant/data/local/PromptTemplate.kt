package top.brzjomo.aitextselectionassistant.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prompt_templates")
data class PromptTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val position: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)