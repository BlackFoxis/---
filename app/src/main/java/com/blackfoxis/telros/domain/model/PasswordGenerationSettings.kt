package com.blackfoxis.telros.domain.model

data class PasswordGenerationSettings (
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeDigits: Boolean = true,
    val includeSymbols: Boolean = true,
    val length: Int = 12
)

fun PasswordGenerationSettings.toSymbolsString(): String {
    return (if (this.includeUppercase) "U" else "") +
            (if (this.includeLowercase) "L" else "") +
            (if (this.includeDigits) "D" else "") +
            (if (this.includeSymbols) "S" else "")
}