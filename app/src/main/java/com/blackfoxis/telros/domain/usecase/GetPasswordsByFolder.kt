package com.blackfoxis.telros.domain.usecase

import com.blackfoxis.telros.domain.model.Password
import com.blackfoxis.telros.domain.repository.PasswordRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPasswordsByFolder @Inject constructor(
    private val repository: PasswordRepository
) {
    operator fun invoke(folderName: String): Flow<List<Password>> {
        return repository.getPasswordsByFolder(folderName)
    }
} //Получение списка паролей, которые не принадлежат ни одной папке.