package com.blackfoxis.telros.presentation.screens.generate

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.blackfoxis.telros.presentation.components.CheckboxWithLabel
import com.blackfoxis.telros.presentation.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratePasswordScreen(navController: NavController) {
    val viewModel: GeneratePasswordViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    // Состояние для управления раскрытием выпадающего меню
    var expandedDropdown by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Для выбора файла для импорта
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            var fileName: String? = null
            try {
                context.contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (displayNameIndex != -1) {
                            fileName = cursor.getString(displayNameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ImportFileName", "Error getting file name", e)
            }

            viewModel.onEvent(GeneratePasswordEvent.ImportFileSelected(fileUri, fileName))
        }
    }


    LaunchedEffect(Unit) {
        viewModel.action.collect { action ->
            when (action) {
                is GeneratePasswordAction.RequestOpenFileImport -> {
                    openFileLauncher.launch(arrayOf("text/csv", "*/*"))
                }
            }
        }
    }


    // Сообщение о результате сохранения
    LaunchedEffect(uiState.saveMessage) {
        uiState.saveMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
            viewModel.onEvent(GeneratePasswordEvent.ClearSaveMessage) // Очищаем сообщение после показа
        }
    }

    // Сообщение о результате импорта и переход на страничку с паролями
    LaunchedEffect(uiState.importResultMessage) {
        uiState.importResultMessage?.let { resultMsg ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = resultMsg.message,
                    duration = if (resultMsg.isError) SnackbarDuration.Long else SnackbarDuration.Short,
                    withDismissAction = resultMsg.isError
                )
            }
            if (!resultMsg.isError) {

                navController.navigate(Screen.Dictionary.route) {
                    popUpTo(Screen.Generate.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
            viewModel.onEvent(GeneratePasswordEvent.ClearImportResult) // Очищаем сообщение
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Генерация пароля") })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Применяем padding от Scaffold
                .padding(16.dp), // Ваш обычный padding для контента внутри LazyColumn
        ) {
            item {
                Text("Настройки генерации", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                CheckboxWithLabel(
                    label = "Большие буквы (A-Z)",
                    checked = uiState.options.includeUppercase,
                    onCheckedChange = { checked ->
                        viewModel.onEvent(
                            GeneratePasswordEvent.UpdateOptions(
                                uiState.options.copy(includeUppercase = checked)
                            )
                        )
                    }
                )
            }

            item {
                CheckboxWithLabel(
                    label = "Маленькие буквы (a-z)",
                    checked = uiState.options.includeLowercase,
                    onCheckedChange = { checked ->
                        viewModel.onEvent(
                            GeneratePasswordEvent.UpdateOptions(
                                uiState.options.copy(includeLowercase = checked)
                            )
                        )
                    }
                )
            }

            item {
                CheckboxWithLabel(
                    label = "Цифры (0-9)",
                    checked = uiState.options.includeDigits,
                    onCheckedChange = { checked ->
                        viewModel.onEvent(
                            GeneratePasswordEvent.UpdateOptions(
                                uiState.options.copy(includeDigits = checked)
                            )
                        )
                    }
                )
            }

            item {
                CheckboxWithLabel(
                    label = "Символы (!@#...)",
                    checked = uiState.options.includeSymbols,
                    onCheckedChange = { checked ->
                        viewModel.onEvent(
                            GeneratePasswordEvent.UpdateOptions(
                                uiState.options.copy(includeSymbols = checked)
                            )
                        )
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Длина: ${uiState.options.length}")
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = uiState.options.length.toFloat(),
                        onValueChange = { newValue ->
                            viewModel.onEvent(
                                GeneratePasswordEvent.UpdateOptions(
                                    uiState.options.copy(length = newValue.toInt())
                                )
                            )
                        },
                        valueRange = 4f..32f,
                        steps = 27
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Button(
                    onClick = { viewModel.onEvent(GeneratePasswordEvent.GenerateNewPassword) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = with(uiState.options) {
                        (includeUppercase || includeLowercase || includeDigits || includeSymbols) && length > 0
                    } && !uiState.isGenerating // Блокируем во время генерации
                ) {
                    if (uiState.isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Сгенерировать")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Отображаем сгенерированный пароль и информацию из uiState
            if (uiState.generatedPasswordValue.isNotEmpty()) {
                item { Text("Сгенерированный пароль:", style = MaterialTheme.typography.titleMedium) }
                item { Text(uiState.generatedPasswordValue, style = MaterialTheme.typography.bodyLarge) }
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { Text(
                    "Энтропия: %.2f бит (алфавит: %d)".format(uiState.entropyValue, uiState.charsetSizeForInfo),
                    style = MaterialTheme.typography.bodyMedium
                ) }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                item { Text("Папка для сохранения (необязательно):", style = MaterialTheme.typography.titleSmall) }
                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = uiState.folderNameInput,
                            onValueChange = { newText ->
                                viewModel.onEvent(GeneratePasswordEvent.FolderNameChanged(newText))
                            },
                            label = { Text("Имя папки или выберите существующую") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown)
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            singleLine = true,
                            readOnly = uiState.existingFolders.isEmpty() && !expandedDropdown
                        )

                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            if (uiState.existingFolders.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("(Без папки / Новая)") }, // Уточнил текст
                                    onClick = {
                                        viewModel.onEvent(GeneratePasswordEvent.FolderSelected(null))
                                        expandedDropdown = false
                                    }
                                )
                            }
                            uiState.existingFolders.forEach { folder ->
                                DropdownMenuItem(
                                    text = { Text(folder) },
                                    onClick = {
                                        viewModel.onEvent(
                                            GeneratePasswordEvent.FolderSelected(
                                                folder
                                            )
                                        )
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    Button(
                        onClick = {
                            viewModel.onEvent(GeneratePasswordEvent.SaveGeneratedPassword)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving // Блокируем во время сохранения
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Сохранить пароль")
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Button(
                    onClick = { viewModel.onEvent(GeneratePasswordEvent.InitiateImport) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isImporting // Блокируем во время импорта
                ) {
                    if (uiState.isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Импорт",
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Импортировать пароли из файла")
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Button(
                    onClick = {
                        navController.navigate(Screen.Dictionary.route) {
                            popUpTo(Screen.Generate.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Все пароли")
                }
            }
        }
    }
}


