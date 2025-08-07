package com.blackfoxis.telros.domain.repository

import com.blackfoxis.telros.domain.model.Password
import kotlinx.coroutines.flow.Flow

interface PasswordRepository {

    suspend fun insertPassword(password: Password)

    suspend fun deletePassword(password: Password)

    suspend fun deleteFolder(folderName: String)

    fun getFolders(): Flow<List<String>>

    fun getPasswordsWithoutFolder(): Flow<List<Password>>

    fun getPasswordsByFolder(folderName: String): Flow<List<Password>>

}