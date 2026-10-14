package com.safevault.app

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    /**
     * Converts a list of [VaultItem] models to an RFC 4180 compliant CSV string,
     * decrypting all passwords and sensitive notes.
     */
    fun exportToCsv(items: List<VaultItem>): String {
        val sb = StringBuilder()
        // Header Row
        sb.append("Title,Portal,Username,Password,Category,Notes,Favorite,CreatedAt,UpdatedAt\n")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        for (item in items) {
            val decryptedPass = SecurityManager.decrypt(item.passwordEncrypted, item.passwordIv)
            val decryptedNotes = if (item.notesEncrypted.isNotEmpty()) {
                SecurityManager.decrypt(item.notesEncrypted, item.notesIv)
            } else {
                ""
            }

            val createdStr = dateFormat.format(Date(item.createdAt))
            val updatedStr = dateFormat.format(Date(item.updatedAt))

            sb.append(escapeCsvField(item.title)).append(",")
            sb.append(escapeCsvField(item.portal)).append(",")
            sb.append(escapeCsvField(item.username)).append(",")
            sb.append(escapeCsvField(decryptedPass)).append(",")
            sb.append(escapeCsvField(item.category)).append(",")
            sb.append(escapeCsvField(decryptedNotes)).append(",")
            sb.append(if (item.isFavorite) "true" else "false").append(",")
            sb.append(escapeCsvField(createdStr)).append(",")
            sb.append(escapeCsvField(updatedStr)).append("\n")
        }

        return sb.toString()
    }

    /**
     * Escapes field value according to RFC 4180:
     * Encloses field in double quotes if it contains comma, newline, carriage return, or double quote.
     * Replaces internal double quotes with pair of double quotes ("").
     */
    fun escapeCsvField(value: String): String {
        if (value.isEmpty()) return ""
        val containsSpecial = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
        return if (containsSpecial) {
            val escaped = value.replace("\"", "\"\"")
            "\"$escaped\""
        } else {
            value
        }
    }

    fun generateExportFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "SafeVault_Export_$timestamp.csv"
    }
}
