package com.blackfoxis.telros.presentation.screens.generate

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blackfoxis.telros.domain.model.Password
import com.blackfoxis.telros.domain.model.PasswordGenerationSettings
import com.blackfoxis.telros.domain.model.toSymbolsString
import com.blackfoxis.telros.domain.usecase.GeneratePassword
import com.blackfoxis.telros.domain.usecase.GetFolders
import com.blackfoxis.telros.domain.usecase.ImportPasswords
import com.blackfoxis.telros.domain.usecase.SavePassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

// UI State
data class GeneratePasswordUiState(
    val options: PasswordGenerationSettings = PasswordGenerationSettings(),
    val generatedPasswordValue: String = "",
    val entropyValue: Double = 0.0,
    val charsetSizeForInfo: Int = 0,

    val folderNameInput: String = "",
    val existingFolders: List<String> = emptyList(),
    val selectedFolder: String? = null,

    val isLoadingFolders: Boolean = true,
    val isGenerating: Boolean = false,
    val isSaving: Boolean = false,
    val saveMessage: String? = null,

    val importResultMessage: ImportResultMessage? = null,
    val isImporting: Boolean = false
)


sealed interface GeneratePasswordEvent {
    data class UpdateOptions(val newOptions: PasswordGenerationSettings) : GeneratePasswordEvent
    object GenerateNewPassword : GeneratePasswordEvent
    object SaveGeneratedPassword : GeneratePasswordEvent
    data class FolderNameChanged(val name: String) : GeneratePasswordEvent
    data class FolderSelected(val folderName: String?) : GeneratePasswordEvent
    object InitiateImport : GeneratePasswordEvent
    data class ImportFileSelected(val uri: Uri, val fileName: String?) : GeneratePasswordEvent
    object ClearSaveMessage : GeneratePasswordEvent
    object ClearImportResult : GeneratePasswordEvent
}


sealed class GeneratePasswordAction {
    object RequestOpenFileImport : GeneratePasswordAction()

}

// Сообщение о результате импорта
data class ImportResultMessage(val message: String, val isError: Boolean = false)
@HiltViewModel
class GeneratePasswordViewModel @Inject constructor(
    private val generatePasswordUseCase: GeneratePassword,
    private val savePasswordUseCase: SavePassword,
    private val getFoldersUseCase: GetFolders,
    private val importPasswordsUseCase: ImportPasswords
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeneratePasswordUiState())
    val uiState: StateFlow<GeneratePasswordUiState> = _uiState.asStateFlow()

    // Для команд к UI
    private val _action = MutableSharedFlow<GeneratePasswordAction>()
    val action: SharedFlow<GeneratePasswordAction> = _action.asSharedFlow()

    init {
        loadExistingFolders()
    }

    private fun loadExistingFolders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFolders = true) }
            getFoldersUseCase()
                .catch { e ->
                    // Обработка ошибки загрузки папок
                    _uiState.update { it.copy(isLoadingFolders = false ) }
                }
                .collect { folders ->
                    _uiState.update { it.copy(existingFolders = folders, isLoadingFolders = false) }
                }
        }
    }

    fun onEvent(event: GeneratePasswordEvent) {
        when (event) {
            is GeneratePasswordEvent.UpdateOptions -> {
                _uiState.update { it.copy(options = event.newOptions) }
            }
            is GeneratePasswordEvent.GenerateNewPassword -> handleGeneratePassword()
            is GeneratePasswordEvent.SaveGeneratedPassword -> handleSavePassword()
            is GeneratePasswordEvent.FolderNameChanged -> handleFolderNameChange(event.name)
            is GeneratePasswordEvent.FolderSelected -> handleFolderSelected(event.folderName)
            is GeneratePasswordEvent.InitiateImport -> initiatePasswordImport()
            is GeneratePasswordEvent.ImportFileSelected -> importPasswordsFromFile(event.uri, event.fileName)
            is GeneratePasswordEvent.ClearSaveMessage -> _uiState.update { it.copy(saveMessage = null) }
            is GeneratePasswordEvent.ClearImportResult -> _uiState.update { it.copy(importResultMessage = null) }
        }
    }

    private fun handleFolderNameChange(name: String) {
        _uiState.update { currentState ->
            val newSelectedFolder = if (currentState.existingFolders.contains(name)) name else null
            currentState.copy(folderNameInput = name, selectedFolder = newSelectedFolder)
        }
    }

    private fun handleFolderSelected(folderName: String?) {
        _uiState.update { currentState ->
            val newFolderNameInput = folderName ?: currentState.folderNameInput
            currentState.copy(selectedFolder = folderName, folderNameInput = newFolderNameInput)
        }
    }

    private fun handleGeneratePassword() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            val currentSettings = _uiState.value.options // Это PasswordGenerationSettings

            try {
                // Вызываем UseCase
                val result = generatePasswordUseCase(currentSettings) // Возвращает GeneratedPasswordResult

                _uiState.update {
                    it.copy(
                        generatedPasswordValue = result.value,
                        entropyValue = result.entropy,
                        charsetSizeForInfo = result.charsetSize,
                        isGenerating = false
                    )
                }
            } catch (e: IllegalArgumentException) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        generatedPasswordValue = "Ошибка: ${e.message}",
                        entropyValue = 0.0
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        generatedPasswordValue = "Не удалось сгенерировать",
                        entropyValue = 0.0
                    )
                }
            }
        }
    }

    private fun handleSavePassword() {
        val state = _uiState.value
        if (state.generatedPasswordValue.isEmpty() || state.generatedPasswordValue.startsWith("Ошибка")) {
            _uiState.update { it.copy(saveMessage = "Нет пароля для сохранения или пароль содержит ошибку.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, saveMessage = null) }

        val finalFolderName = state.selectedFolder ?: state.folderNameInput.trim().takeIf { it.isNotEmpty() }

        val passwordToSave = Password(
            value = state.generatedPasswordValue,
            entropy = state.entropyValue,
            symbols = state.options.toSymbolsString(),
            folderName = finalFolderName,
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            try {
                savePasswordUseCase(passwordToSave)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveMessage = "Пароль сохранен" + (finalFolderName?.let { fn -> " в '$fn'" } ?: ""),
                        generatedPasswordValue = "", // Очищаем после сохранения
                        entropyValue = 0.0,
                        charsetSizeForInfo = 0,
                        folderNameInput = "", // Очищаем папку
                        selectedFolder = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveMessage = "Ошибка сохранения: ${e.message}") }
            }
        }
    }

    private fun initiatePasswordImport() {
        viewModelScope.launch {
            _action.emit(GeneratePasswordAction.RequestOpenFileImport)
        }
    }

    private fun importPasswordsFromFile(uri: Uri, fileName: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importResultMessage = null) }
            try {
                val result = importPasswordsUseCase(ImportPasswords.Params(uri, fileName))

                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importResultMessage = ImportResultMessage(
                            message = result.messages.joinToString("\n"),
                            isError = result.errorCount > 0 && result.successCount == 0
                        )
                    )
                }
                if (result.successCount > 0 || result.importedToFolderName != null) {
                    loadExistingFolders() // Обновляем список папок, если были изменения
                }
            } catch (e: IOException) {
                _uiState.update { it.copy(isImporting = false, importResultMessage = ImportResultMessage("Ошибка чтения файла: ${e.message}", true)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isImporting = false, importResultMessage = ImportResultMessage("Ошибка импорта: ${e.message}", true)) }
            }
        }
    }
}

