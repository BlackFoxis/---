package com.blackfoxis.telros.domain.usecase

import com.blackfoxis.telros.domain.repository.PasswordRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


class GetFolders @Inject constructor(
    private val repository: PasswordRepository
) {
    operator fun invoke(): Flow<List<String>> {
        return repository.getFolders()
    }
} //Получение списка всех имен папок.