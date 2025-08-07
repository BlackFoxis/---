package com.blackfoxis.telros.domain.usecase

import com.blackfoxis.telros.domain.model.Password
import com.blackfoxis.telros.domain.repository.PasswordRepository
import javax.inject.Inject

class SavePassword @Inject constructor(
    private val repository: PasswordRepository
) {
    suspend operator fun invoke(password: Password) {
        if (password.value.isBlank()) {
            throw IllegalArgumentException("Password value cannot be blank.")
        }
        repository.insertPassword(password)
    }
}