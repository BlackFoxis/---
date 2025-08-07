package com.blackfoxis.telros.data.local.mapper

import com.blackfoxis.telros.data.local.PasswordEntity
import com.blackfoxis.telros.domain.model.Password
import javax.inject.Inject

fun PasswordEntity.toDomain(): Password {
    return Password(
        id = this.id,
        value = this.value,
        entropy = this.entropy,
        symbols = this.symbols,
        folderName = this.folderName,
        createdAt = this.createdAt
    )
}

fun Password.toEntity(): PasswordEntity {
    return PasswordEntity(
        id = this.id,
        value = this.value,
        entropy = this.entropy,
        symbols = this.symbols,
        folderName = this.folderName,
        createdAt = this.createdAt
    )
}