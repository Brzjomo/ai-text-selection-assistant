package top.brzjomo.aitextselectionassistant.data.repository

import kotlinx.coroutines.flow.Flow
import top.brzjomo.aitextselectionassistant.data.local.ApiProvider
import top.brzjomo.aitextselectionassistant.data.local.ApiProviderDao

class ApiProviderRepository(private val apiProviderDao: ApiProviderDao) {

    fun getAllProviders(): Flow<List<ApiProvider>> = apiProviderDao.getAll()

    suspend fun getProviderById(id: Long): ApiProvider? = apiProviderDao.getById(id)

    suspend fun getDefaultProvider(): ApiProvider? = apiProviderDao.getDefault()

    suspend fun insertProvider(apiProvider: ApiProvider): Long {
        val id = apiProviderDao.insert(apiProvider)
        if (apiProvider.isDefault) {
            apiProviderDao.setDefault(id)
        }
        return id
    }

    suspend fun updateProvider(apiProvider: ApiProvider) {
        apiProviderDao.update(apiProvider)
        if (apiProvider.isDefault) {
            apiProviderDao.setDefault(apiProvider.id)
        }
    }

    suspend fun deleteProvider(apiProvider: ApiProvider) = apiProviderDao.delete(apiProvider)

    suspend fun deleteProviderById(id: Long) = apiProviderDao.deleteById(id)

    suspend fun setDefaultProvider(id: Long) {
        apiProviderDao.clearDefault()
        apiProviderDao.setDefault(id)
    }
}