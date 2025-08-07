package com.blackfoxis.telros.data.repository


import com.blackfoxis.telros.data.local.PasswordDao
import com.blackfoxis.telros.data.local.mapper.toDomain
import com.blackfoxis.telros.data.local.mapper.toEntity
import com.blackfoxis.telros.domain.model.Password
import com.blackfoxis.telros.domain.repository.PasswordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PasswordRepositoryImpl @Inject constructor(
    private val dao: PasswordDao
) : PasswordRepository {

    override suspend fun insertPassword(password: Password) {
        dao.insert(password.toEntity())
    }

    override suspend fun deletePassword(password: Password) {
        dao.delete(password.toEntity())
    }

    override suspend fun deleteFolder(folderName: String) {
        dao.deletePasswordsByFolderName(folderName)
    }

    override fun getFolders(): Flow<List<String>> {
        return dao.getFoldersFlow()
    }

    override fun getPasswordsWithoutFolder(): Flow<List<Password>> {
        return dao.getGeneratedFlow().map { entityList ->
            entityList.map { it.toDomain() }
        }
    }

    override fun getPasswordsByFolder(folderName: String): Flow<List<Password>> {
        return dao.getByFolderFlow(folderName).map { entityList ->
            entityList.map { it.toDomain() }
        }
    }
}