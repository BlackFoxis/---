package com.blackfoxis.telros.domain.usecase

import com.blackfoxis.telros.domain.repository.PasswordRepository
import javax.inject.Inject

class DeleteFolder @Inject constructor(
    private val repository: PasswordRepository
) {
    suspend operator fun invoke(folderName: String) {
        repository.deleteFolder(folderName)
    }
}//Удаление папки и всех паролей в ней.