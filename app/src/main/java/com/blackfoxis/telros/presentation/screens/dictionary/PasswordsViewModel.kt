package com.blackfoxis.telros.presentation.screens.dictionary

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blackfoxis.telros.domain.model.Password
import com.blackfoxis.telros.domain.usecase.DeleteFolder
import com.blackfoxis.telros.domain.usecase.DeletePassword
import com.blackfoxis.telros.domain.usecase.GetFolders
import com.blackfoxis.telros.domain.usecase.GetPasswordsByFolder
import com.blackfoxis.telros.domain.usecase.GetPasswordsWithoutFolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Writer
import javax.inject.Inject

// UI State
data class PasswordsUiState(
    val isLoadingInitialData: Boolean = true,
    val folders: List<String> = emptyList(),
    val passwordsWithoutFolder: List<Password> = emptyList(),

    val expandedFolderName: String? = null,
    val passwordsInExpandedFolder: List<Password> = emptyList(),
    val isLoadingFolderContent: Boolean = false,

    val error: String? = null, // Общая ошибка
    val folderError: String? = null, // Ошибка загрузки папки
    val deleteError: String? = null, // Ошибка удаления

    val isExporting: Boolean = false,
    val exportMessage: String? = null,

    val folderToDelete: String? = null
)


sealed interface PasswordsEvent {
    data class FolderClicked(val folderName: String) : PasswordsEvent
    data class DeletePasswordClicked(val password: Password) : PasswordsEvent
    data class DeleteFolderClicked(val folderName: String) : PasswordsEvent // Удаляем папку?
    object ConfirmDeleteFolder : PasswordsEvent // Да
    object CancelDeleteFolder : PasswordsEvent  // Нет
    object InitiateExport : PasswordsEvent
    data class ExportToFileRequested(val uri: Uri) : PasswordsEvent // После выбора файла
    object ClearError : PasswordsEvent
    object ClearExportMessage : PasswordsEvent
}

// Single actions to UI
sealed class PasswordsAction {
    data class RequestCreateFileForExport(val fileName: String, val fileMimeType: String) : PasswordsAction()
}

@HiltViewModel
class PasswordsViewModel @Inject constructor(
    private val getFoldersUseCase: GetFolders,
    private val getPasswordsWithoutFolderUseCase: GetPasswordsWithoutFolder,
    private val getPasswordsByFolderUseCase: GetPasswordsByFolder,
    private val deletePasswordUseCase: DeletePassword,
    private val deleteFolderUseCase: DeleteFolder
) : ViewModel() {
    private val _uiState = MutableStateFlow(PasswordsUiState())
    val uiState: StateFlow<PasswordsUiState> = _uiState.asStateFlow()

    private val _action = MutableSharedFlow<PasswordsAction>()
    val action: SharedFlow<PasswordsAction> = _action.asSharedFlow()

    private var folderContentJob: Job? = null

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        getFoldersUseCase()
            .combine(getPasswordsWithoutFolderUseCase()) { folders, passwordsWithoutFolder ->
                val currentExpanded = _uiState.value.expandedFolderName
                val newExpandedFolderName = if (currentExpanded != null && !folders.contains(currentExpanded)) {
                    folderContentJob?.cancel() // Если раскрытая папка удалена, отменяем ее Job
                    null
                } else {
                    currentExpanded
                }
                val newPasswordsInExpanded = when (newExpandedFolderName) {
                    null -> {
                        emptyList()
                    }
                    _uiState.value.expandedFolderName -> {
                        _uiState.value.passwordsInExpandedFolder // Сохраняем, если папка та же
                    }
                    else -> {
                        // Если папка сменилась (но не была удалена), контент загрузится по клику
                        emptyList()
                    }
                }

                _uiState.value.copy(
                    isLoadingInitialData = false,
                    folders = folders,
                    passwordsWithoutFolder = passwordsWithoutFolder,
                    expandedFolderName = newExpandedFolderName,
                    passwordsInExpandedFolder = newPasswordsInExpanded,

                    isLoadingFolderContent = if (newExpandedFolderName == _uiState.value.expandedFolderName)
                        _uiState.value.isLoadingFolderContent else false,
                    error = null
                )
            }
            .catch { e ->
                _uiState.update { it.copy(isLoadingInitialData = false, error = "Ошибка загрузки данных: ${e.message}") }
            }
            .onEach { newState -> _uiState.value = newState } // Обновляем состояние
            .launchIn(viewModelScope)
    }

    fun onEvent(event: PasswordsEvent) {
        when (event) {
            is PasswordsEvent.FolderClicked -> handleFolderClicked(event.folderName)
            is PasswordsEvent.DeletePasswordClicked -> handleDeletePassword(event.password)
            is PasswordsEvent.DeleteFolderClicked -> _uiState.update { it.copy(folderToDelete = event.folderName) }
            is PasswordsEvent.ConfirmDeleteFolder -> handleConfirmDeleteFolder()
            is PasswordsEvent.CancelDeleteFolder -> _uiState.update { it.copy(folderToDelete = null, deleteError = null) }
            is PasswordsEvent.InitiateExport -> handleInitiateExport()
            is PasswordsEvent.ExportToFileRequested -> exportPasswordsToFile(event.uri)
            is PasswordsEvent.ClearError -> _uiState.update { it.copy(error = null, folderError = null, deleteError = null) }
            is PasswordsEvent.ClearExportMessage -> _uiState.update { it.copy(exportMessage = null) }
        }
    }

    private fun handleFolderClicked(folderName: String) {
        folderContentJob?.cancel()
        val currentState = _uiState.value

        if (currentState.expandedFolderName == folderName) { // Клик по уже раскрытой папке (сворачиваем)
            _uiState.update {
                it.copy(
                    expandedFolderName = null,
                    passwordsInExpandedFolder = emptyList(),
                    isLoadingFolderContent = false,
                    folderError = null
                )
            }
        } else { // Раскрываем новую папку
            _uiState.update {
                it.copy(
                    expandedFolderName = folderName,
                    isLoadingFolderContent = true,
                    passwordsInExpandedFolder = emptyList(), // Очищаем от предыдущей
                    folderError = null
                )
            }
            folderContentJob = getPasswordsByFolderUseCase(folderName)
                .catch { e ->
                    // Обновляем состояние, только если эта папка все еще выбрана
                    if (_uiState.value.expandedFolderName == folderName) {
                        _uiState.update {
                            it.copy(
                                isLoadingFolderContent = false,
                                folderError = "Ошибка загрузки '${folderName}': ${e.message}"
                            )
                        }
                    }
                }
                .onEach { passwordsInFolder ->
                    if (_uiState.value.expandedFolderName == folderName) {
                        _uiState.update {
                            it.copy(
                                passwordsInExpandedFolder = passwordsInFolder,
                                isLoadingFolderContent = false
                            )
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    private fun handleDeletePassword(password: Password) {
        viewModelScope.launch {
            try {
                deletePasswordUseCase(password)
                _uiState.update { it.copy(deleteError = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(deleteError = "Ошибка удаления пароля: ${e.message}") }
            }
        }
    }

    private fun handleConfirmDeleteFolder() {
        val folderNameToDelete = _uiState.value.folderToDelete ?: return
        viewModelScope.launch {
            try {
                deleteFolderUseCase(folderNameToDelete)

                _uiState.update { it.copy(folderToDelete = null, deleteError = null) } // Закрываем диалог и сбрасываем ошибку
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        folderToDelete = null, // Все равно закрываем диалог
                        deleteError = "Ошибка удаления папки '$folderNameToDelete': ${e.message}"
                    )
                }
            }
        }
    }

    private fun handleInitiateExport() {
        val currentState = _uiState.value
        val passwordsToExport: List<Password>
        val defaultFileName: String

        if (currentState.expandedFolderName != null) {
            passwordsToExport = currentState.passwordsInExpandedFolder
            defaultFileName = "passwords_${currentState.expandedFolderName.replace(" ", "_").lowercase()}.csv"
        } else {
            passwordsToExport = currentState.passwordsWithoutFolder
            defaultFileName = "passwords_unfiled.csv"
        }

        if (passwordsToExport.isEmpty()) {
            _uiState.update { it.copy(exportMessage = "Нет паролей для экспорта.", isExporting = false) }
            return
        }

        viewModelScope.launch {
            _action.emit(PasswordsAction.RequestCreateFileForExport(defaultFileName, "text/csv"))
        }
    }

    @Inject
    lateinit var application: Application // Инжектируем Application

    private fun exportPasswordsToFile(uri: Uri) {
        val contentResolver = application.contentResolver
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportMessage = null) }
            val currentState = _uiState.value
            val passwordsToExport = if (currentState.expandedFolderName != null) {
                currentState.passwordsInExpandedFolder
            } else {
                currentState.passwordsWithoutFolder
            }

            if (passwordsToExport.isEmpty()) {
                _uiState.update { it.copy(isExporting = false, exportMessage = "Нет паролей для экспорта.") }
                return@launch
            }

            var outputStream: OutputStream?
            try {
                outputStream = contentResolver.openOutputStream(uri) // Присваиваем значение

                if (outputStream == null) { // Явная проверка на null
                    throw IOException("Не удалось открыть OutputStream для Uri: $uri")
                }

                val writer: Writer = OutputStreamWriter(outputStream)

                writer.buffered().use { bufferedFileWriter ->
                    bufferedFileWriter.appendLine("Value,Entropy,Symbols,FolderName,CreatedAt")
                    passwordsToExport.forEach { password ->
                        val value = "\"${password.value.replace("\"", "\"\"")}\""
                        val entropy = password.entropy.toString()
                        val symbols = "\"${password.symbols.replace("\"", "\"\"")}\""
                        val folder = "\"${password.folderName?.replace("\"", "\"\"") ?: ""}\""
                        val createdAt = password.createdAt.toString()
                        bufferedFileWriter.appendLine("$value,$entropy,$symbols,$folder,$createdAt")
                    }
                }

                _uiState.update { it.copy(isExporting = false, exportMessage = "Пароли успешно экспортированы!") }

            } catch (e: IOException) {
                _uiState.update { it.copy(isExporting = false, exportMessage = "Ошибка экспорта: ${e.message}") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, exportMessage = "Непредвиденная ошибка экспорта: ${e.message}") }
            } finally { }
        }
    }

    override fun onCleared() {
        super.onCleared()
        folderContentJob?.cancel()
    }
}