package com.blackfoxis.telros.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blackfoxis.telros.domain.model.Password

//Карточка пароля
@Composable
fun PasswordItem(
    password: Password,
    onCopyClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = "Пароль",
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = password.value,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                IconButton(onClick = { onCopyClick(password.value) }) {
                    Icon(
                        Icons.Filled.Add, // Иконка для копирования
                        contentDescription = "Копировать пароль"
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Filled.Delete, contentDescription = "Удалить пароль")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            password.entropy?.let {
                Text(
                    text = "Энтропия: %.2f бит".format(it),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            password.symbols?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = "Набор: $it",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            Text(
                text = "Создан: ${
                    java.text.SimpleDateFormat("dd.MM.yy HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(password.createdAt))
                }",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}