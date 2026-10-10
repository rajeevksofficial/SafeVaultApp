package com.safevault.app

import java.security.SecureRandom

object PasswordUtils {

    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?"

    private val random = SecureRandom()

    enum class StrengthLevel(val label: String, val colorRes: Int, val bgRes: Int) {
        WEAK("Weak", R.color.security_crimson, R.drawable.bg_pill_strength_weak),
        MEDIUM("Medium", R.color.security_amber, R.drawable.bg_pill_strength_medium),
        STRONG("Strong", R.color.security_emerald, R.drawable.bg_pill_strength_strong),
        VERY_STRONG("Very Strong", R.color.brand_primary, R.drawable.bg_pill_strength_strong)
    }

    data class StrengthResult(
        val level: StrengthLevel,
        val scorePercent: Int
    )

    fun generatePassword(
        length: Int = 16,
        includeUpper: Boolean = true,
        includeLower: Boolean = true,
        includeDigits: Boolean = true,
        includeSymbols: Boolean = true
    ): String {
        val pools = mutableListOf<String>()
        val requiredChars = mutableListOf<Char>()

        if (includeUpper) {
            pools.add(UPPERCASE)
            requiredChars.add(UPPERCASE[random.nextInt(UPPERCASE.length)])
        }
        if (includeLower) {
            pools.add(LOWERCASE)
            requiredChars.add(LOWERCASE[random.nextInt(LOWERCASE.length)])
        }
        if (includeDigits) {
            pools.add(DIGITS)
            requiredChars.add(DIGITS[random.nextInt(DIGITS.length)])
        }
        if (includeSymbols) {
            pools.add(SYMBOLS)
            requiredChars.add(SYMBOLS[random.nextInt(SYMBOLS.length)])
        }

        if (pools.isEmpty()) {
            return generatePassword(length, includeUpper = false, includeLower = true, includeDigits = true, includeSymbols = false)
        }

        val allChars = pools.joinToString("")
        val remainingLength = (length - requiredChars.size).coerceAtLeast(0)

        val generatedChars = ArrayList<Char>(length)
        generatedChars.addAll(requiredChars)

        for (i in 0 until remainingLength) {
            generatedChars.add(allChars[random.nextInt(allChars.length)])
        }

        // Shuffle characters securely
        for (i in generatedChars.indices.reversed()) {
            val j = random.nextInt(i + 1)
            val temp = generatedChars[i]
            generatedChars[i] = generatedChars[j]
            generatedChars[j] = temp
        }

        return generatedChars.joinToString("")
    }

    fun evaluateStrength(password: String): StrengthResult {
        if (password.isEmpty()) {
            return StrengthResult(StrengthLevel.WEAK, 0)
        }

        var score = 0
        val len = password.length

        // Length score
        when {
            len >= 16 -> score += 40
            len >= 12 -> score += 30
            len >= 8 -> score += 18
            else -> score += 8
        }

        var hasUpper = false
        var hasLower = false
        var hasDigit = false
        var hasSymbol = false

        for (c in password) {
            when {
                c.isUpperCase() -> hasUpper = true
                c.isLowerCase() -> hasLower = true
                c.isDigit() -> hasDigit = true
                else -> hasSymbol = true
            }
        }

        var varietyCount = 0
        if (hasUpper) { score += 15; varietyCount++ }
        if (hasLower) { score += 15; varietyCount++ }
        if (hasDigit) { score += 15; varietyCount++ }
        if (hasSymbol) { score += 15; varietyCount++ }

        if (varietyCount >= 4 && len >= 12) {
            score = (score + 10).coerceAtMost(100)
        }

        val level = when {
            score >= 85 -> StrengthLevel.VERY_STRONG
            score >= 65 -> StrengthLevel.STRONG
            score >= 40 -> StrengthLevel.MEDIUM
            else -> StrengthLevel.WEAK
        }

        return StrengthResult(level, score.coerceIn(5, 100))
    }
}
