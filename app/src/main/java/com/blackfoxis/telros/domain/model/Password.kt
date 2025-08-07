package com.blackfoxis.telros.domain.model

data class Password(
    val id: Int = 0,
    val value: String,
    val entropy: Double,
    val symbols: String,
    val folderName: String? = null,
    val createdAt: Long
)