package com.safevault.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class CsvImporterTest {

    @Test
    fun testParseStandardSafeVaultCsv() {
        val csv = "Title,Portal,Username,Password,Category,Notes,Favorite\n" +
                "Google Workspace,gmail.com,john@gmail.com,Secret#Pass123,Email,Backup codes here,true\n" +
                "Facebook Account,facebook.com,john.fb,Fb#2026Secure!,Social,Main profile,false\n" +
                "HDFC NetBanking,hdfcbank.com,cust_123,Bank#Pass456,Banking,Customer ID: 123,1"

        val result = CsvImporter.parseCsv(ByteArrayInputStream(csv.toByteArray()))
        assertEquals(3, result.validItems.size)
        assertEquals(0, result.skippedCount)

        val item1 = result.validItems[0]
        assertEquals("Google Workspace", item1.title)
        assertEquals("gmail.com", item1.portal)
        assertEquals("john@gmail.com", item1.username)
        assertEquals("Email", item1.category)
        assertTrue(item1.isFavorite)

        // Verify password and notes are encrypted
        assertNotEquals("Secret#Pass123", item1.passwordEncrypted)
        assertTrue(item1.passwordEncrypted.isNotEmpty())
        assertTrue(item1.passwordIv.isNotEmpty())
        val decryptedPass = SecurityManager.decrypt(item1.passwordEncrypted, item1.passwordIv)
        assertEquals("Secret#Pass123", decryptedPass)

        val decryptedNotes = SecurityManager.decrypt(item1.notesEncrypted, item1.notesIv)
        assertEquals("Backup codes here", decryptedNotes)
    }

    @Test
    fun testParseChromePasswordsCsv() {
        val csv = "name,url,username,password,note\n" +
                "GitHub,https://github.com/login,gituser,Git#Secret888!,Personal access token notes\n" +
                "Netflix,https://www.netflix.com/,watcher@home.com,Stream&4KPass,Family plan"

        val result = CsvImporter.parseCsv(ByteArrayInputStream(csv.toByteArray()))
        assertEquals(2, result.validItems.size)

        val item0 = result.validItems[0]
        assertEquals("GitHub", item0.title)
        assertEquals("github.com", item0.portal)
        assertEquals("gituser", item0.username)
        assertEquals("Work", item0.category) // Inferred from github

        val pass = SecurityManager.decrypt(item0.passwordEncrypted, item0.passwordIv)
        assertEquals("Git#Secret888!", pass)
    }

    @Test
    fun testParseBitwardenCsv() {
        val csv = "folder,favorite,type,name,notes,fields,reprompt,login_uri,login_username,login_password,login_totp\n" +
                "Finance,1,login,SBI Online,Account notes,,,https://www.onlinesbi.sbi,sbi_user,Sbi#Secure99!,"

        val result = CsvImporter.parseCsv(ByteArrayInputStream(csv.toByteArray()))
        assertEquals(1, result.validItems.size)

        val item = result.validItems[0]
        assertEquals("SBI Online", item.title)
        assertEquals("onlinesbi.sbi", item.portal)
        assertEquals("sbi_user", item.username)
        assertEquals("Banking", item.category) // Inferred
        assertTrue(item.isFavorite)

        val pass = SecurityManager.decrypt(item.passwordEncrypted, item.passwordIv)
        assertEquals("Sbi#Secure99!", pass)
    }

    @Test
    fun testParseCsvWithCommasInsideQuotesAndEscapedQuotes() {
        val csv = "Title,Portal,Username,Password,Category,Notes\n" +
                "\"Complex, Inc.\",\"portal.com\",\"john,admin\",\"P@ss,word\"\"123\",Work,\"Note with, comma and \"\"quotes\"\"\""

        val result = CsvImporter.parseCsv(ByteArrayInputStream(csv.toByteArray()))
        assertEquals(1, result.validItems.size)

        val item = result.validItems[0]
        assertEquals("Complex, Inc.", item.title)
        assertEquals("portal.com", item.portal)
        assertEquals("john,admin", item.username)

        val pass = SecurityManager.decrypt(item.passwordEncrypted, item.passwordIv)
        assertEquals("P@ss,word\"123", pass)

        val note = SecurityManager.decrypt(item.notesEncrypted, item.notesIv)
        assertEquals("Note with, comma and \"quotes\"", note)
    }

    @Test
    fun testParseCsvWithMultilineNotes() {
        val csv = "Title,Portal,Username,Password,Category,Notes\n" +
                "My Service,service.com,myuser,pass123,Other,\"Line 1\nLine 2\nLine 3\""

        val result = CsvImporter.parseCsv(ByteArrayInputStream(csv.toByteArray()))
        assertEquals(1, result.validItems.size)

        val item = result.validItems[0]
        val note = SecurityManager.decrypt(item.notesEncrypted, item.notesIv)
        assertTrue(note.contains("Line 1"))
        assertTrue(note.contains("Line 2"))
        assertTrue(note.contains("Line 3"))
    }

    @Test
    fun testSampleCsvTemplateValidity() {
        val sample = CsvImporter.getSampleCsvTemplate()
        val result = CsvImporter.parseCsv(ByteArrayInputStream(sample.toByteArray()))
        assertTrue(result.validItems.size >= 5)
        assertEquals(0, result.skippedCount)
    }
}
