package com.blackfoxis.telros.domain.usecase

import com.blackfoxis.telros.domain.model.GeneratedPasswordResult
import com.blackfoxis.telros.domain.model.PasswordGenerationSettings
import javax.inject.Inject
import kotlin.math.log2

class GeneratePassword @Inject constructor() {

    operator fun invoke(settings: PasswordGenerationSettings): GeneratedPasswordResult {
        val charset = buildCharset(settings)
        if (charset.isEmpty() || settings.length <= 0) {
            return GeneratedPasswordResult("", 0.0, 0)
        }

        val password = (1..settings.length)
            .map { charset.random() }
            .joinToString("")

        val entropy = calculateEntropy(password, charset.length)
        return GeneratedPasswordResult(password, entropy, charset.length)
    }

    private fun buildCharset(settings: PasswordGenerationSettings): String {
        var charset = ""
        if (settings.includeUppercase) charset += "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        if (settings.includeLowercase) charset += "abcdefghijklmnopqrstuvwxyz"
        if (settings.includeDigits) charset += "0123456789"
        if (settings.includeSymbols) charset += "!@#\$%^&*()-_=+[]{};:,.<>?/\\|"
        return charset
    }

    private fun calculateEntropy(password: String, charSetSize: Int): Double {
        if (charSetSize <= 1 || password.isEmpty()) return 0.0
        return password.length * log2(charSetSize.toDouble())
    }
}