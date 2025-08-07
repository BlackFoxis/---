package com.blackfoxis.telros.domain.usecase

import android.app.Application
import android.net.Uri
import com.blackfoxis.telros.domain.model.ImportResult
import com.blackfoxis.telros.domain.model.Password
import com.blackfoxis.telros.domain.repository.PasswordRepository
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import javax.inject.Inject

class ImportPasswords @Inject constructor(
    private val application: Application, // Инжектируем Application для доступа к ContentResolver
    private val repository: PasswordRepository
) {

    data class Params(val uri: Uri, val fileNameFromUri: String?)

    suspend operator fun invoke(params: Params): ImportResult {
        var importedCount = 0
        var errorCount = 0
        val resultMessages = mutableListOf<String>()

        val folderNameFromFile = params.fileNameFromUri?.substringBeforeLast('.')?.trim()?.takeIf { it.isNotBlank() }

        try {
            application.contentResolver.openInputStream(params.uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    if (line == null) {
                        resultMessages.add("Файл пуст или некорректный заголовок.")
                        return ImportResult(0, 1, resultMessages, folderNameFromFile)
                    }


                    if (!line.contains("Value", ignoreCase = true)) {
                        resultMessages.add("Некорректный формат CSV заголовка (ожидается 'Value').")
                        return ImportResult(0, 1, resultMessages, folderNameFromFile)
                    }

                    val headerParts = line.split(',').map { it.trim().removeSurrounding("\"") }
                    val valueIndex = headerParts.indexOfFirst { it.equals("Value", ignoreCase = true) }
                    val entropyIndex = headerParts.indexOfFirst { it.equals("Entropy", ignoreCase = true) }
                    val symbolsIndex = headerParts.indexOfFirst { it.equals("Symbols", ignoreCase = true) }


                    if (valueIndex == -1) {
                        resultMessages.add("Обязательное поле 'Value' не найдено в заголовке CSV.")
                        return ImportResult(0, 1, resultMessages, folderNameFromFile)
                    }

                    line = reader.readLine()
                    while (line != null) {
                        val currentLine = line // для сообщений об ошибках

                        val parts = currentLine.split(',').map { it.trim().removeSurrounding("\"") }
                        try {
                            if (parts.size <= valueIndex) { // Проверка, что есть хотя бы поле для value
                                resultMessages.add("Недостаточно столбцов для поля 'Value' в строке: $currentLine")
                                errorCount++
                                line = reader.readLine()
                                continue
                            }

                            val value = parts.getOrNull(valueIndex)?.takeIf { it.isNotBlank() }
                            if (value == null) {
                                resultMessages.add("Пустое значение пароля в строке: $currentLine")
                                errorCount++
                                line = reader.readLine()
                                continue
                            }

                            val entropyStr = if (entropyIndex != -1) parts.getOrNull(entropyIndex) else null
                            val entropyValue: Double = entropyStr?.toDoubleOrNull() ?: 0.0

                            val symbolsValue: String = (if (symbolsIndex != -1) parts.getOrNull(symbolsIndex) else null)
                                ?.takeIf { it.isNotBlank() } ?: determineSymbolsFromValue(value)

                            val password = Password(
                                value = value,
                                entropy = entropyValue,
                                symbols = symbolsValue,
                                folderName = folderNameFromFile,
                                createdAt = System.currentTimeMillis()

                            )
                            repository.insertPassword(password)
                            importedCount++
                        } catch (e: Exception) {
                            resultMessages.add("Ошибка парсинга строки '$currentLine': ${e.message}")
                            errorCount++
                        }
                        line = reader.readLine()
                    }
                }
            } ?: run {
                resultMessages.add("Не удалось открыть файл для чтения.")
                errorCount++
            }
        } catch (e: IOException) {
            resultMessages.add("Ошибка чтения файла: ${e.message}")
            errorCount++
        } catch (e: Exception) {
            resultMessages.add("Непредвиденная ошибка импорта: ${e.message}")
            errorCount++
        }

        if (importedCount > 0 && errorCount == 0) {
            resultMessages.add(0, "Импорт успешно завершен.") // Добавляем в начало списка
        } else if (importedCount > 0 && errorCount > 0) {
            resultMessages.add(0, "Импорт завершен с ошибками.")
        } else if (errorCount > 0) {
            resultMessages.add(0, "Импорт не удался.")
        }


        return ImportResult(importedCount, errorCount, resultMessages, folderNameFromFile)
    }


    private fun determineSymbolsFromValue(value: String): String {
        val hasUpper = value.any { it.isUpperCase() }
        val hasLower = value.any { it.isLowerCase() }
        val hasDigit = value.any { it.isDigit() }

        val hasSymbol = value.any { !it.isLetterOrDigit() }

        return (if (hasUpper) "U" else "") +
                (if (hasLower) "L" else "") +
                (if (hasDigit) "D" else "") +
                (if (hasSymbol) "S" else "")
    }
}