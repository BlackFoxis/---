package com.blackfoxis.telros.presentation.screens.dictionary

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.blackfoxis.telros.presentation.components.FolderItem
import com.blackfoxis.telros.presentation.components.PasswordItem
import com.blackfoxis.telros.presentation.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordsScreen(
    navController: NavController,
    viewModel: PasswordsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Для создания файла при экспорте
    val createFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.onEvent(PasswordsEvent.ExportToFileRequested(it)) // Отправляем событие
        }
    }

    //Выбираем имя файла
    LaunchedEffect(Unit) {
        viewModel.action.collect { action ->
            when (action) {
                is PasswordsAction.RequestCreateFileForExport -> {
                    createFileLauncher.launch(action.fileName)
                }
            }
        }
    }

    // Соо об экспорте
    LaunchedEffect(uiState.exportMessage) {
        uiState.exportMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
            viewModel.onEvent(PasswordsEvent.ClearExportMessage) // Очищаем сообщение
        }
    }

    // Ошибки
    val errorMessages = listOfNotNull(uiState.error, uiState.folderError, uiState.deleteError)
    LaunchedEffect(errorMessages) {
        errorMessages.firstOrNull()?.let { errorMessage ->
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            viewModel.onEvent(PasswordsEvent.ClearError) // Очищаем все ошибки
        }
    }


    // Диалог подтверждения удаления папки
    if (uiState.folderToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(PasswordsEvent.CancelDeleteFolder) },
            title = { Text("Удалить папку?") },
            text = { Text("Вы уверены, что хотите удалить папку \"${uiState.folderToDelete}\" и все пароли в ней?") },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(PasswordsEvent.ConfirmDeleteFolder) }) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(PasswordsEvent.CancelDeleteFolder) }) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Словари и пароли") },
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(PasswordsEvent.InitiateExport) },
                        enabled = !uiState.isExporting
                    ) {
                        if (uiState.isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Экспорт паролей"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate(Screen.Generate.route)
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Создать новый пароль")
            }
        }
    ) { paddingValues ->
        if (uiState.isLoadingInitialData && uiState.folders.isEmpty() && uiState.passwordsWithoutFolder.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.folders.isEmpty() && uiState.passwordsWithoutFolder.isEmpty() && !uiState.isLoadingInitialData) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет доступных паролей или папок.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Секция для папок
                if (uiState.folders.isNotEmpty()) {
                    item {
                        Text(
                            text = "Папки",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(items = uiState.folders, key = { folderName -> "folder-$folderName" }) { folderName ->
                        val isExpanded = uiState.expandedFolderName == folderName

                        FolderItem(
                            folderName = folderName,
                            isExpanded = isExpanded,
                            onFolderClick = {
                                viewModel.onEvent(PasswordsEvent.FolderClicked(folderName))
                            },
                            onDeleteFolderClick = {
                                viewModel.onEvent(PasswordsEvent.DeleteFolderClicked(folderName))
                            }
                        )

                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)) {
                                if (uiState.isLoadingFolderContent && isExpanded) {
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                                } else if (uiState.passwordsInExpandedFolder.isEmpty() && isExpanded) {
                                    Text("Папка пуста", style = MaterialTheme.typography.bodySmall)
                                } else {
                                    uiState.passwordsInExpandedFolder.forEach { passwordInFolder ->
                                        PasswordItem(
                                            password = passwordInFolder,
                                            onCopyClick = { passwordValue ->
                                                clipboardManager.setText(AnnotatedString(passwordValue))
                                                Toast.makeText(context, "Пароль скопирован!", Toast.LENGTH_SHORT).show()
                                            },
                                            onDeleteClick = {
                                                viewModel.onEvent(PasswordsEvent.DeletePasswordClicked(passwordInFolder))
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Секция для паролей без папок
                if (uiState.passwordsWithoutFolder.isNotEmpty()) {
                    item {
                        Text(
                            text = "Пароли без папки",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(items = uiState.passwordsWithoutFolder, key = { password -> "password-nofolder-${password.id}" }) { password ->
                        PasswordItem(
                            password = password,
                            onCopyClick = { passwordValue ->
                                clipboardManager.setText(AnnotatedString(passwordValue))
                                Toast.makeText(context, "Пароль скопирован!", Toast.LENGTH_SHORT).show()
                            },
                            onDeleteClick = {
                                viewModel.onEvent(PasswordsEvent.DeletePasswordClicked(password))
                            }
                        )
                    }
                }
            }
        }
    }
}



