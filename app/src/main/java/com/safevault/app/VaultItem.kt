package com.safevault.app

import java.io.Serializable

data class VaultItem(
    val id: Long = 0,
    val title: String,
    val portal: String,
    val username: String,
    val passwordEncrypted: String,
    val passwordIv: String,
    val category: String = "Other",
    val notesEncrypted: String = "",
    val notesIv: String = "",
    val passwordStrength: String = "MEDIUM",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Serializable
