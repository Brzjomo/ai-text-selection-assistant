package top.brzjomo.aitextselectionassistant.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiProviderDao {
    @Query("SELECT * FROM api_providers ORDER BY is_default DESC, updated_at DESC")
    fun getAll(): Flow<List<ApiProvider>>

    @Query("SELECT * FROM api_providers WHERE id = :id")
    suspend fun getById(id: Long): ApiProvider?

    @Query("SELECT * FROM api_providers WHERE is_default = 1 LIMIT 1")
    suspend fun getDefault(): ApiProvider?

    @Insert
    suspend fun insert(apiProvider: ApiProvider): Long

    @Update
    suspend fun update(apiProvider: ApiProvider)

    @Delete
    suspend fun delete(apiProvider: ApiProvider)

    @Query("DELETE FROM api_providers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE api_providers SET is_default = 0")
    suspend fun clearDefault()

    @Query("UPDATE api_providers SET is_default = 1 WHERE id = :id")
    suspend fun setDefault(id: Long)
}