package com.blackfoxis.telros.domain.usecase

import com.blackfoxis.telros.domain.model.Password
import com.blackfoxis.telros.domain.repository.PasswordRepository
import javax.inject.Inject

class DeletePassword @Inject constructor(
    private val repository: PasswordRepository
) {
    suspend operator fun invoke(password: Password) {
        repository.deletePassword(password)
    }
} //Удаление пароля.