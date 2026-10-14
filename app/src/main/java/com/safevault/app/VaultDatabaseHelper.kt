package com.safevault.app

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class VaultDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "safevault.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_ITEMS = "vault_items"
        private const val COL_ID = "id"
        private const val COL_TITLE = "title"
        private const val COL_PORTAL = "portal"
        private const val COL_USERNAME = "username"
        private const val COL_PASSWORD_ENCRYPTED = "password_encrypted"
        private const val COL_PASSWORD_IV = "password_iv"
        private const val COL_CATEGORY = "category"
        private const val COL_NOTES_ENCRYPTED = "notes_encrypted"
        private const val COL_NOTES_IV = "notes_iv"
        private const val COL_PASSWORD_STRENGTH = "password_strength"
        private const val COL_IS_FAVORITE = "is_favorite"
        private const val COL_CREATED_AT = "created_at"
        private const val COL_UPDATED_AT = "updated_at"
    }

    data class VaultStats(
        val totalCount: Int,
        val favoriteCount: Int,
        val strongCount: Int
    )

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSql = """
            CREATE TABLE $TABLE_ITEMS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT NOT NULL,
                $COL_PORTAL TEXT NOT NULL,
                $COL_USERNAME TEXT NOT NULL,
                $COL_PASSWORD_ENCRYPTED TEXT NOT NULL,
                $COL_PASSWORD_IV TEXT NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_NOTES_ENCRYPTED TEXT,
                $COL_NOTES_IV TEXT,
                $COL_PASSWORD_STRENGTH TEXT,
                $COL_IS_FAVORITE INTEGER DEFAULT 0,
                $COL_CREATED_AT INTEGER,
                $COL_UPDATED_AT INTEGER
            )
        """.trimIndent()
        db.execSQL(createTableSql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ITEMS")
        onCreate(db)
    }

    fun insert(item: VaultItem): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, item.title)
            put(COL_PORTAL, item.portal)
            put(COL_USERNAME, item.username)
            put(COL_PASSWORD_ENCRYPTED, item.passwordEncrypted)
            put(COL_PASSWORD_IV, item.passwordIv)
            put(COL_CATEGORY, item.category)
            put(COL_NOTES_ENCRYPTED, item.notesEncrypted)
            put(COL_NOTES_IV, item.notesIv)
            put(COL_PASSWORD_STRENGTH, item.passwordStrength)
            put(COL_IS_FAVORITE, if (item.isFavorite) 1 else 0)
            put(COL_CREATED_AT, item.createdAt)
            put(COL_UPDATED_AT, item.updatedAt)
        }
        return db.insert(TABLE_ITEMS, null, values)
    }

    fun insertBatch(items: List<VaultItem>): Int {
        val db = writableDatabase
        var count = 0
        db.beginTransaction()
        try {
            for (item in items) {
                val values = ContentValues().apply {
                    put(COL_TITLE, item.title)
                    put(COL_PORTAL, item.portal)
                    put(COL_USERNAME, item.username)
                    put(COL_PASSWORD_ENCRYPTED, item.passwordEncrypted)
                    put(COL_PASSWORD_IV, item.passwordIv)
                    put(COL_CATEGORY, item.category)
                    put(COL_NOTES_ENCRYPTED, item.notesEncrypted)
                    put(COL_NOTES_IV, item.notesIv)
                    put(COL_PASSWORD_STRENGTH, item.passwordStrength)
                    put(COL_IS_FAVORITE, if (item.isFavorite) 1 else 0)
                    put(COL_CREATED_AT, item.createdAt)
                    put(COL_UPDATED_AT, item.updatedAt)
                }
                if (db.insert(TABLE_ITEMS, null, values) > 0) {
                    count++
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return count
    }

    fun update(item: VaultItem): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, item.title)
            put(COL_PORTAL, item.portal)
            put(COL_USERNAME, item.username)
            put(COL_PASSWORD_ENCRYPTED, item.passwordEncrypted)
            put(COL_PASSWORD_IV, item.passwordIv)
            put(COL_CATEGORY, item.category)
            put(COL_NOTES_ENCRYPTED, item.notesEncrypted)
            put(COL_NOTES_IV, item.notesIv)
            put(COL_PASSWORD_STRENGTH, item.passwordStrength)
            put(COL_IS_FAVORITE, if (item.isFavorite) 1 else 0)
            put(COL_UPDATED_AT, System.currentTimeMillis())
        }
        val affected = db.update(TABLE_ITEMS, values, "$COL_ID = ?", arrayOf(item.id.toString()))
        return affected > 0
    }

    fun delete(id: Long): Boolean {
        val db = writableDatabase
        val affected = db.delete(TABLE_ITEMS, "$COL_ID = ?", arrayOf(id.toString()))
        return affected > 0
    }

    fun toggleFavorite(id: Long, isFavorite: Boolean): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_IS_FAVORITE, if (isFavorite) 1 else 0)
            put(COL_UPDATED_AT, System.currentTimeMillis())
        }
        val affected = db.update(TABLE_ITEMS, values, "$COL_ID = ?", arrayOf(id.toString()))
        return affected > 0
    }

    fun getItemById(id: Long): VaultItem? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_ITEMS,
            null,
            "$COL_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        return cursor.use {
            if (it.moveToFirst()) mapCursorToVaultItem(it) else null
        }
    }

    fun getAllItems(query: String? = null, category: String? = null, favoritesOnly: Boolean = false): List<VaultItem> {
        val db = readableDatabase
        val conditions = mutableListOf<String>()
        val args = mutableListOf<String>()

        if (!query.isNullOrBlank()) {
            val q = "%${query.trim()}%"
            conditions.add("($COL_TITLE LIKE ? OR $COL_PORTAL LIKE ? OR $COL_USERNAME LIKE ?)")
            args.add(q)
            args.add(q)
            args.add(q)
        }

        if (!category.isNullOrBlank() && category != "All Items") {
            conditions.add("$COL_CATEGORY = ?")
            args.add(category)
        }

        if (favoritesOnly) {
            conditions.add("$COL_IS_FAVORITE = 1")
        }

        val whereClause = if (conditions.isNotEmpty()) conditions.joinToString(" AND ") else null
        val whereArgs = if (args.isNotEmpty()) args.toTypedArray() else null

        val cursor = db.query(
            TABLE_ITEMS,
            null,
            whereClause,
            whereArgs,
            null,
            null,
            "$COL_IS_FAVORITE DESC, $COL_UPDATED_AT DESC"
        )

        val items = mutableListOf<VaultItem>()
        cursor.use {
            while (it.moveToNext()) {
                items.add(mapCursorToVaultItem(it))
            }
        }
        return items
    }

    fun getVaultStats(): VaultStats {
        val db = readableDatabase
        var total = 0
        var favorites = 0
        var strong = 0

        val cursor = db.rawQuery(
            "SELECT COUNT(*), SUM(CASE WHEN $COL_IS_FAVORITE = 1 THEN 1 ELSE 0 END), SUM(CASE WHEN $COL_PASSWORD_STRENGTH IN ('STRONG', 'VERY_STRONG') THEN 1 ELSE 0 END) FROM $TABLE_ITEMS",
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                total = it.getInt(0)
                favorites = it.getInt(1)
                strong = it.getInt(2)
            }
        }
        return VaultStats(total, favorites, strong)
    }

    fun deleteAll(): Boolean {
        val db = writableDatabase
        return db.delete(TABLE_ITEMS, null, null) > 0
    }

    private fun mapCursorToVaultItem(cursor: Cursor): VaultItem {
        return VaultItem(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)),
            portal = cursor.getString(cursor.getColumnIndexOrThrow(COL_PORTAL)),
            username = cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)),
            passwordEncrypted = cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD_ENCRYPTED)),
            passwordIv = cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD_IV)),
            category = cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)),
            notesEncrypted = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTES_ENCRYPTED)) ?: "",
            notesIv = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTES_IV)) ?: "",
            passwordStrength = cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD_STRENGTH)) ?: "MEDIUM",
            isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_FAVORITE)) == 1,
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT)),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_UPDATED_AT))
        )
    }

    fun seedStarterDataIfNeeded() {
        val count = getAllItems().size
        if (count == 0) {
            val fbPass = SecurityManager.encrypt("Fb!SafePass#2026*X")
            val fbNotes = SecurityManager.encrypt("2FA Recovery Codes:\n1. 8492-4921\n2. 5839-2041\nLinked phone: +1 (555) 019-2831")
            insert(
                VaultItem(
                    title = "Facebook Account",
                    portal = "facebook.com",
                    username = "rajeev.user@facebook.com",
                    passwordEncrypted = fbPass.first,
                    passwordIv = fbPass.second,
                    category = "Social",
                    notesEncrypted = fbNotes.first,
                    notesIv = fbNotes.second,
                    passwordStrength = "VERY_STRONG",
                    isFavorite = true
                )
            )

            val gmPass = SecurityManager.encrypt("Gmeil#Vault99&Safe!")
            val gmNotes = SecurityManager.encrypt("Primary Google Workspace & Gmail.\nSecurity Question: First school name -> Greenfield High\nApp Password for IMAP active.")
            insert(
                VaultItem(
                    title = "Google / Gmail Account",
                    portal = "gmail.com",
                    username = "rajeev.official@gmail.com",
                    passwordEncrypted = gmPass.first,
                    passwordIv = gmPass.second,
                    category = "Email",
                    notesEncrypted = gmNotes.first,
                    notesIv = gmNotes.second,
                    passwordStrength = "VERY_STRONG",
                    isFavorite = true
                )
            )

            val netPass = SecurityManager.encrypt("Netflx#Stream&4K!")
            val netNotes = SecurityManager.encrypt("Family Premium Plan.\nBilling renewal on 15th of each month.")
            insert(
                VaultItem(
                    title = "Netflix Streaming",
                    portal = "netflix.com",
                    username = "home.cinema@gmail.com",
                    passwordEncrypted = netPass.first,
                    passwordIv = netPass.second,
                    category = "Other",
                    notesEncrypted = netNotes.first,
                    notesIv = netNotes.second,
                    passwordStrength = "STRONG",
                    isFavorite = false
                )
            )

            val bankPass = SecurityManager.encrypt("Hdfc#NetBank$2026")
            val bankNotes = SecurityManager.encrypt("Customer ID: 89410294\nTransaction Password expires in 180 days.\nDebit Card ATM PIN: 7291 (Encrypted)")
            insert(
                VaultItem(
                    title = "HDFC NetBanking",
                    portal = "hdfcbank.com",
                    username = "cust_id_89410294",
                    passwordEncrypted = bankPass.first,
                    passwordIv = bankPass.second,
                    category = "Banking",
                    notesEncrypted = bankNotes.first,
                    notesIv = bankNotes.second,
                    passwordStrength = "STRONG",
                    isFavorite = true
                )
            )
        }
    }
}
