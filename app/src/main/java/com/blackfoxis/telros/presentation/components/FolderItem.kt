package com.blackfoxis.telros.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp


//Карточка папки
@Composable
fun FolderItem(
    folderName: String,
    isExpanded: Boolean,
    onFolderClick: () -> Unit,
    onDeleteFolderClick: () -> Unit
) {
    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "folderRotation")

    val backgroundColor = if (isExpanded) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isExpanded) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onFolderClick), // Клик по всей карточке для раскрытия/сворачивания
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 4.dp else 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor, contentColor = contentColor)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.AddCircle, // Иконка папки
                contentDescription = "Папка",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = folderName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            // Иконка удаления папки
            IconButton(onClick = {
                onDeleteFolderClick()
            }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Удалить папку",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            // Иконка-стрелка
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                modifier = Modifier.rotate(rotationAngle)
            )
        }
    }
}