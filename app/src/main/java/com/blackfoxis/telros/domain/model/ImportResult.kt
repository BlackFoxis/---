package com.blackfoxis.telros.domain.model

data class ImportResult(
    val successCount: Int,
    val errorCount: Int,
    val messages: List<String>, // Сообщения об ошибках или статусе
    val importedToFolderName: String? // Имя папки, куда был произведен импорт
)