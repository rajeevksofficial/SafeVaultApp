package com.safevault.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class CsvExporterTest {

    @Test
    fun testExportToCsvBasic() {
        val passEnc = SecurityManager.encrypt("MySecretPass123!")
        val notesEnc = SecurityManager.encrypt("Important 2FA codes")

        val item1 = VaultItem(
            id = 1,
            title = "Google Workspace",
            portal = "gmail.com",
            username = "admin@gmail.com",
            passwordEncrypted = passEnc.first,
            passwordIv = passEnc.second,
            category = "Email",
            notesEncrypted = notesEnc.first,
            notesIv = notesEnc.second,
            passwordStrength = "STRONG",
            isFavorite = true
        )

        val csv = CsvExporter.exportToCsv(listOf(item1))
        assertTrue(csv.startsWith("Title,Portal,Username,Password,Category,Notes,Favorite"))
        assertTrue(csv.contains("Google Workspace"))
        assertTrue(csv.contains("gmail.com"))
        assertTrue(csv.contains("admin@gmail.com"))
        assertTrue(csv.contains("MySecretPass123!"))
        assertTrue(csv.contains("Important 2FA codes"))
        assertTrue(csv.contains("true"))
    }

    @Test
    fun testExportFieldEscaping() {
        assertEquals("simple", CsvExporter.escapeCsvField("simple"))
        assertEquals("\"with,comma\"", CsvExporter.escapeCsvField("with,comma"))
        assertEquals("\"with \"\"quotes\"\"\"", CsvExporter.escapeCsvField("with \"quotes\""))
        assertEquals("\"with\nnewline\"", CsvExporter.escapeCsvField("with\nnewline"))
    }

    @Test
    fun testRoundTripExportAndImport() {
        // Create sample items
        val p1 = SecurityManager.encrypt("P@ss1")
        val p2 = SecurityManager.encrypt("P@ss,with,comma and \"quotes\"")

        val items = listOf(
            VaultItem(
                id = 1,
                title = "GitHub",
                portal = "github.com",
                username = "gituser",
                passwordEncrypted = p1.first,
                passwordIv = p1.second,
                category = "Work",
                isFavorite = true
            ),
            VaultItem(
                id = 2,
                title = "Bank Account",
                portal = "bank.com",
                username = "bankuser",
                passwordEncrypted = p2.first,
                passwordIv = p2.second,
                category = "Banking",
                isFavorite = false
            )
        )

        val exportedCsv = CsvExporter.exportToCsv(items)
        val importResult = CsvImporter.parseCsv(ByteArrayInputStream(exportedCsv.toByteArray()))

        assertEquals(2, importResult.validItems.size)
        assertEquals(0, importResult.skippedCount)

        val item0 = importResult.validItems[0]
        assertEquals("GitHub", item0.title)
        assertEquals("github.com", item0.portal)
        assertEquals("gituser", item0.username)
        val decP1 = SecurityManager.decrypt(item0.passwordEncrypted, item0.passwordIv)
        assertEquals("P@ss1", decP1)

        val item1 = importResult.validItems[1]
        assertEquals("Bank Account", item1.title)
        assertEquals("bank.com", item1.portal)
        val decP2 = SecurityManager.decrypt(item1.passwordEncrypted, item1.passwordIv)
        assertEquals("P@ss,with,comma and \"quotes\"", decP2)
    }
}
