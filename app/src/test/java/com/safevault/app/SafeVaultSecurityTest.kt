package com.safevault.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeVaultSecurityTest {

    @Test
    fun testPasswordGeneratorLengths() {
        val lengths = listOf(8, 12, 16, 24, 32)
        for (len in lengths) {
            val pass = PasswordUtils.generatePassword(length = len)
            assertEquals(len, pass.length)
        }
    }

    @Test
    fun testPasswordGeneratorCharacterComposition() {
        val pass = PasswordUtils.generatePassword(
            length = 20,
            includeUpper = true,
            includeLower = true,
            includeDigits = true,
            includeSymbols = true
        )
        assertEquals(20, pass.length)
        assertTrue(pass.any { it.isUpperCase() })
        assertTrue(pass.any { it.isLowerCase() })
        assertTrue(pass.any { it.isDigit() })
        assertTrue(pass.any { "!@#$%^&*()_+-=[]{}|;:,.<>?".contains(it) })
    }

    @Test
    fun testPasswordGeneratorDigitsOnly() {
        val pin = PasswordUtils.generatePassword(
            length = 6,
            includeUpper = false,
            includeLower = false,
            includeDigits = true,
            includeSymbols = false
        )
        assertEquals(6, pin.length)
        assertTrue(pin.all { it.isDigit() })
    }

    @Test
    fun testPasswordStrengthEvaluation() {
        val weak = PasswordUtils.evaluateStrength("123456")
        assertEquals(PasswordUtils.StrengthLevel.WEAK, weak.level)

        val medium = PasswordUtils.evaluateStrength("Password1")
        assertEquals(PasswordUtils.StrengthLevel.MEDIUM, medium.level)

        val strong = PasswordUtils.evaluateStrength("P@ssw0rd2026!")
        assertTrue(strong.scorePercent >= 65)

        val veryStrong = PasswordUtils.evaluateStrength("Xk9!mZ#4vW8@qL2$9a")
        assertEquals(PasswordUtils.StrengthLevel.VERY_STRONG, veryStrong.level)
        assertTrue(veryStrong.scorePercent >= 85)
    }

    @Test
    fun testEncryptionAndDecryptionRoundtrip() {
        val testSecrets = listOf(
            "MySecretFacebookPassword#2026",
            "gmail_super_secret_token_123!",
            "Customer ID: 123456\nPIN: 9876\nSecurity Answer: Fluffy",
            "Special characters: ~!@#$%^&*()_+{}[]:;<>,.?"
        )

        for (secret in testSecrets) {
            val encrypted = SecurityManager.encrypt(secret)
            assertTrue(encrypted.first.isNotEmpty())
            assertTrue(encrypted.second.isNotEmpty())
            assertNotEquals(secret, encrypted.first)

            val decrypted = SecurityManager.decrypt(encrypted.first, encrypted.second)
            assertEquals(secret, decrypted)
        }
    }

    @Test
    fun testUniqueIVPerEncryption() {
        val secret = "IdenticalPlaintextPassword"
        val enc1 = SecurityManager.encrypt(secret)
        val enc2 = SecurityManager.encrypt(secret)

        // Semantic security: IVs must be distinct
        assertNotEquals(enc1.second, enc2.second)
        // Ciphertexts must differ due to distinct IVs
        assertNotEquals(enc1.first, enc2.first)

        // Both decrypt back to identical plaintext
        assertEquals(secret, SecurityManager.decrypt(enc1.first, enc1.second))
        assertEquals(secret, SecurityManager.decrypt(enc2.first, enc2.second))
    }

    @Test
    fun testMasterPinHashingAndVerification() {
        val salt = ByteArray(16) { 0x42.toByte() }
        val pin = "1234"
        val hash1 = SecurityManager.hashPinForTest(pin, salt)
        val hash2 = SecurityManager.hashPinForTest(pin, salt)

        // Deterministic hash with same salt
        assertEquals(hash1, hash2)
        assertTrue(SecurityManager.slowEqualsForTest(hash1, hash2))

        // Different PIN with same salt produces distinct hash
        val wrongPinHash = SecurityManager.hashPinForTest("9999", salt)
        assertNotEquals(hash1, wrongPinHash)
        assertTrue(!SecurityManager.slowEqualsForTest(hash1, wrongPinHash))

        // Same PIN with different salt produces distinct hash
        val salt2 = ByteArray(16) { 0x24.toByte() }
        val hashWithDifferentSalt = SecurityManager.hashPinForTest(pin, salt2)
        assertNotEquals(hash1, hashWithDifferentSalt)
    }

    @Test
    fun testMasterPinChangeSimulation() {
        // Step 1: Initial PIN = "1234"
        val oldSalt = ByteArray(16) { 1 }
        val oldPin = "1234"
        var currentHash = SecurityManager.hashPinForTest(oldPin, oldSalt)
        var currentSalt = oldSalt

        // Verify current PIN before change
        val candidateOldPin = "1234"
        val candidateOldHash = SecurityManager.hashPinForTest(candidateOldPin, currentSalt)
        assertTrue(SecurityManager.slowEqualsForTest(currentHash, candidateOldHash))

        // Wrong current PIN fails
        val wrongCurrentPin = "0000"
        val wrongCurrentHash = SecurityManager.hashPinForTest(wrongCurrentPin, currentSalt)
        assertTrue(!SecurityManager.slowEqualsForTest(currentHash, wrongCurrentHash))

        // Step 2 & 3: Change to new PIN = "5678" with fresh salt
        val newSalt = ByteArray(16) { 2 }
        val newPin = "5678"
        currentHash = SecurityManager.hashPinForTest(newPin, newSalt)
        currentSalt = newSalt

        // Now old PIN no longer matches
        val oldPinAgainstNewState = SecurityManager.hashPinForTest(oldPin, currentSalt)
        assertTrue(!SecurityManager.slowEqualsForTest(currentHash, oldPinAgainstNewState))

        // New PIN correctly matches
        val newPinAgainstNewState = SecurityManager.hashPinForTest(newPin, currentSalt)
        assertTrue(SecurityManager.slowEqualsForTest(currentHash, newPinAgainstNewState))
    }
}
