package com.blackfoxis.telros.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passwords")
data class PasswordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val value: String,
    val entropy: Double,
    val symbols: String,
    val folderName: String? = null, // null — значит "без папки"
    val createdAt: Long = System.currentTimeMillis()
)