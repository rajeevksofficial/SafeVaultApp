package com.safevault.app

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale

object CsvImporter {

    data class CsvImportResult(
        val totalRowsParsed: Int,
        val validItems: List<VaultItem>,
        val skippedCount: Int,
        val sampleItemTitles: List<String> = emptyList(),
        val errorMessages: List<String> = emptyList()
    )

    /**
     * Parses a CSV input stream (RFC 4180 compliant) and converts valid rows
     * into encrypted [VaultItem] models ready for database insertion.
     */
    fun parseCsv(inputStream: InputStream): CsvImportResult {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val fullContent = reader.readText().removePrefix("\uFEFF") // Strip UTF-8 BOM if present

        val rows = parseCsvRows(fullContent)
        if (rows.isEmpty()) {
            return CsvImportResult(0, emptyList(), 0, emptyList(), listOf("The selected CSV file is empty."))
        }

        // Determine if first row is a header
        val firstRow = rows[0]
        val headerMap = detectHeaders(firstRow)
        val hasHeaders = headerMap.isNotEmpty()

        val dataRows = if (hasHeaders) rows.drop(1) else rows
        val validItems = mutableListOf<VaultItem>()
        var skippedCount = 0
        val errorMessages = mutableListOf<String>()
        val sampleTitles = mutableListOf<String>()

        for ((index, row) in dataRows.withIndex()) {
            val rowNumber = if (hasHeaders) index + 2 else index + 1
            if (row.all { it.isBlank() }) {
                continue // Skip empty blank lines
            }

            var title = ""
            var portal = ""
            var username = ""
            var password = ""
            var category = ""
            var notes = ""
            var isFavorite = false

            if (hasHeaders) {
                title = headerMap["title"]?.let { row.getOrNull(it) }?.trim() ?: ""
                portal = headerMap["portal"]?.let { row.getOrNull(it) }?.trim() ?: ""
                username = headerMap["username"]?.let { row.getOrNull(it) }?.trim() ?: ""
                password = headerMap["password"]?.let { row.getOrNull(it) }?.trim() ?: ""
                category = headerMap["category"]?.let { row.getOrNull(it) }?.trim() ?: ""
                notes = headerMap["notes"]?.let { row.getOrNull(it) }?.trim() ?: ""
                val favStr = headerMap["favorite"]?.let { row.getOrNull(it) }?.trim()?.lowercase(Locale.ROOT) ?: ""
                isFavorite = favStr in listOf("true", "1", "yes", "y")
            } else {
                // Default positional mapping: title, portal, username, password, category, notes, isFavorite
                title = row.getOrNull(0)?.trim() ?: ""
                portal = row.getOrNull(1)?.trim() ?: ""
                username = row.getOrNull(2)?.trim() ?: ""
                password = row.getOrNull(3)?.trim() ?: ""
                category = row.getOrNull(4)?.trim() ?: ""
                notes = row.getOrNull(5)?.trim() ?: ""
                val favStr = row.getOrNull(6)?.trim()?.lowercase(Locale.ROOT) ?: ""
                isFavorite = favStr in listOf("true", "1", "yes", "y")
            }

            // Sanitization and smart inference
            if (title.isBlank() && portal.isNotBlank()) {
                title = extractDomainName(portal)
            } else if (title.isBlank() && username.isNotBlank()) {
                title = "Account ($username)"
            }

            if (portal.isBlank() && title.isNotBlank()) {
                portal = if (title.contains(".")) title else "${title.lowercase(Locale.ROOT).replace(" ", "")}.com"
            } else if (portal.isNotBlank()) {
                portal = extractDomainName(portal)
            }

            // If completely empty of identifiers and password, skip row
            if (title.isBlank() && username.isBlank() && password.isBlank()) {
                skippedCount++
                errorMessages.add("Row $rowNumber skipped: Missing title, username, and password.")
                continue
            }

            val finalCategory = sanitizeOrInferCategory(category, title, portal)
            val passEncryption = SecurityManager.encrypt(password)
            val notesEncryption = if (notes.isNotBlank()) SecurityManager.encrypt(notes) else Pair("", "")
            val strength = PasswordUtils.evaluateStrength(password).level.name

            val item = VaultItem(
                title = title.ifBlank { "Imported Credential" },
                portal = portal.ifBlank { "unknown.com" },
                username = username,
                passwordEncrypted = passEncryption.first,
                passwordIv = passEncryption.second,
                category = finalCategory,
                notesEncrypted = notesEncryption.first,
                notesIv = notesEncryption.second,
                passwordStrength = strength,
                isFavorite = isFavorite
            )

            validItems.add(item)
            if (sampleTitles.size < 5) {
                sampleTitles.add(item.title)
            }
        }

        return CsvImportResult(
            totalRowsParsed = dataRows.size,
            validItems = validItems,
            skippedCount = skippedCount,
            sampleItemTitles = sampleTitles,
            errorMessages = errorMessages
        )
    }

    /**
     * RFC 4180 compliant CSV state machine parser.
     * Accurately supports quoted fields, escaped quotes (""), commas, and embedded newlines.
     */
    fun parseCsvRows(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var insideQuotes = false
        var i = 0
        val len = text.length

        while (i < len) {
            val c = text[i]
            when {
                c == '"' -> {
                    if (insideQuotes && i + 1 < len && text[i + 1] == '"') {
                        // Escaped quote: "" -> "
                        currentField.append('"')
                        i++ // Skip second quote
                    } else {
                        // Toggle quoted state
                        insideQuotes = !insideQuotes
                    }
                }
                c == ',' && !insideQuotes -> {
                    currentRow.add(currentField.toString())
                    currentField.setLength(0)
                }
                (c == '\n' || c == '\r') && !insideQuotes -> {
                    // Handle CRLF or single LF/CR
                    if (c == '\r' && i + 1 < len && text[i + 1] == '\n') {
                        i++
                    }
                    currentRow.add(currentField.toString())
                    currentField.setLength(0)
                    rows.add(ArrayList(currentRow))
                    currentRow.clear()
                }
                else -> {
                    currentField.append(c)
                }
            }
            i++
        }

        // Add last field and row if any
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            rows.add(currentRow)
        }

        return rows
    }

    /**
     * Maps CSV column header names to standard VaultItem properties.
     * Supports formats from Bitwarden, Chrome, LastPass, KeePass, 1Password, and custom templates.
     */
    private fun detectHeaders(row: List<String>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()

        for ((index, col) in row.withIndex()) {
            val norm = col.trim().lowercase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "")

            when {
                norm in listOf("title", "name", "account", "service", "label", "application", "app") && !map.containsKey("title") -> {
                    map["title"] = index
                }
                norm in listOf("portal", "url", "website", "loginuri", "link", "domain", "host", "site", "address") && !map.containsKey("portal") -> {
                    map["portal"] = index
                }
                norm in listOf("username", "login", "loginusername", "user", "email", "loginname", "userid") && !map.containsKey("username") -> {
                    map["username"] = index
                }
                norm in listOf("password", "pass", "loginpassword", "secret", "pwd") && !map.containsKey("password") -> {
                    map["password"] = index
                }
                norm in listOf("category", "folder", "grouping", "group", "type") && !map.containsKey("category") -> {
                    map["category"] = index
                }
                norm in listOf("notes", "note", "comments", "extra", "description", "memo") && !map.containsKey("notes") -> {
                    map["notes"] = index
                }
                norm in listOf("favorite", "fav", "isfavorite", "starred") && !map.containsKey("favorite") -> {
                    map["favorite"] = index
                }
            }
        }

        // Must at least identify either password or username or portal/url to consider as a valid header row
        val isRecognized = map.containsKey("password") || map.containsKey("username") || map.containsKey("portal")
        return if (isRecognized) map else emptyMap()
    }

    /**
     * Cleans up domain names from URLs (e.g. "https://www.github.com/login" -> "github.com").
     */
    private fun extractDomainName(rawUrl: String): String {
        return try {
            var s = rawUrl.trim()
            if (s.startsWith("http://", ignoreCase = true)) s = s.substring(7)
            if (s.startsWith("https://", ignoreCase = true)) s = s.substring(8)
            s = s.substringBefore("/").substringBefore(":")
            if (s.startsWith("www.", ignoreCase = true)) s = s.substring(4)
            s.ifBlank { rawUrl }
        } catch (_: Exception) {
            rawUrl
        }
    }

    /**
     * Validates category against allowed app categories, or auto-infers from title and portal.
     */
    private fun sanitizeOrInferCategory(category: String, title: String, portal: String): String {
        val validCategories = listOf("Social", "Email", "Banking", "Work", "Shopping", "Other")
        val match = validCategories.find { it.equals(category.trim(), ignoreCase = true) }
        if (match != null) return match

        val text = "$title $portal $category".lowercase(Locale.ROOT)
        return when {
            text.containsAny("facebook", "fb.com", "instagram", "twitter", "x.com", "linkedin", "reddit", "tiktok", "snapchat", "discord", "telegram", "pinterest", "social") -> "Social"
            text.containsAny("gmail", "google", "yahoo", "outlook", "hotmail", "proton", "icloud", "mail", "email") -> "Email"
            text.containsAny("bank", "hdfc", "icici", "sbi", "chase", "paypal", "stripe", "paytm", "credit", "debit", "fintech", "axis", "kotak", "banking") -> "Banking"
            text.containsAny("github", "gitlab", "slack", "jira", "confluence", "aws", "azure", "trello", "zoom", "microsoft", "office365", "notion", "work") -> "Work"
            text.containsAny("amazon", "flipkart", "ebay", "walmart", "target", "aliexpress", "etsy", "myntra", "shopping") -> "Shopping"
            else -> "Other"
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }

    /**
     * Provides a standard template CSV format with sample records.
     */
    fun getSampleCsvTemplate(): String {
        return """
            Title,Portal,Username,Password,Category,Notes,Favorite
            Google Workspace,gmail.com,user.name@gmail.com,Pass#Google2026!,Email,2FA recovery codes in safe,true
            Facebook Profile,facebook.com,john.doe,Fb#StrongPass2026,Social,Linked with mobile number,false
            GitHub Account,github.com,johndoe,Git#DevSecure99!,Work,SSH Key on office laptop,true
            HDFC NetBanking,hdfcbank.com,cust_89410294,Bank#VaultPass88!,Banking,Customer ID: 89410294,true
            Amazon Shopping,amazon.com,john.doe@gmail.com,Amz#SecureBuy77!,Shopping,Prime delivery account,false
            Netflix Streaming,netflix.com,home.cinema@gmail.com,Netflx#Stream&4K!,Other,Family 4K plan,false
        """.trimIndent()
    }
}
