package top.brzjomo.aitextselectionassistant.data.repository

import kotlinx.coroutines.flow.Flow
import top.brzjomo.aitextselectionassistant.data.local.PromptTemplate
import top.brzjomo.aitextselectionassistant.data.local.PromptTemplateDao

class PromptTemplateRepository(private val promptTemplateDao: PromptTemplateDao) {

    fun getAllTemplates(): Flow<List<PromptTemplate>> = promptTemplateDao.getAll()

    suspend fun getTemplateById(id: Long): PromptTemplate? = promptTemplateDao.getById(id)

    suspend fun insertTemplate(promptTemplate: PromptTemplate): Long = promptTemplateDao.insert(promptTemplate)

    suspend fun updateTemplate(promptTemplate: PromptTemplate) = promptTemplateDao.update(promptTemplate)

    suspend fun deleteTemplate(promptTemplate: PromptTemplate) = promptTemplateDao.delete(promptTemplate)

    suspend fun deleteTemplateById(id: Long) = promptTemplateDao.deleteById(id)
}