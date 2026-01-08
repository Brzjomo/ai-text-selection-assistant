package top.brzjomo.aitextselectionassistant.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptTemplateDao {
    @Query("SELECT * FROM prompt_templates ORDER BY position ASC")
    fun getAll(): Flow<List<PromptTemplate>>

    @Query("SELECT * FROM prompt_templates WHERE id = :id")
    suspend fun getById(id: Long): PromptTemplate?

    @Insert
    suspend fun insert(promptTemplate: PromptTemplate): Long

    @Update
    suspend fun update(promptTemplate: PromptTemplate)

    @Delete
    suspend fun delete(promptTemplate: PromptTemplate)

    @Query("DELETE FROM prompt_templates WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE prompt_templates SET position = :position, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int, updatedAt: Long)
}
