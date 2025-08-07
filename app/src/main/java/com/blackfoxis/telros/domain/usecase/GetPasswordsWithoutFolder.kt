package com.blackfoxis.telros.domain.usecase

import com.blackfoxis.telros.domain.model.Password
import com.blackfoxis.telros.domain.repository.PasswordRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPasswordsWithoutFolder @Inject constructor(
    private val repository: PasswordRepository
) {
    operator fun invoke(): Flow<List<Password>> {
        return repository.getPasswordsWithoutFolder()
    }
} //Получение списка паролей для конкретной папки.