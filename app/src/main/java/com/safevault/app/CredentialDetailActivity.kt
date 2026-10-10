package com.safevault.app

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.safevault.app.databinding.ActivityCredentialDetailBinding
import java.util.Locale

class CredentialDetailActivity : BaseSecureActivity() {

    private lateinit var binding: ActivityCredentialDetailBinding
    private lateinit var dbHelper: VaultDatabaseHelper

    private var itemId: Long = -1L
    private var currentItem: VaultItem? = null
    private var isPasswordRevealed = false
    private var decryptedPassword = ""
    private var decryptedNotes = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCredentialDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = VaultDatabaseHelper(this)
        itemId = intent.getLongExtra("item_id", -1L)

        if (itemId == -1L) {
            Toast.makeText(this, "Invalid item", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        val item = dbHelper.getItemById(itemId)
        if (item == null) {
            finish()
            return
        }
        currentItem = item

        // Decrypt password & notes
        decryptedPassword = SecurityManager.decrypt(item.passwordEncrypted, item.passwordIv)
        decryptedNotes = if (item.notesEncrypted.isNotEmpty()) {
            SecurityManager.decrypt(item.notesEncrypted, item.notesIv)
        } else {
            ""
        }

        // Header info
        binding.tvDetailTitle.text = item.title
        binding.tvDetailPortal.text = item.portal
        val initial = if (item.title.isNotEmpty()) item.title.first().uppercase() else "V"
        binding.tvDetailAvatarInitial.text = initial
        binding.layoutDetailAvatar.background.setTint(getPortalColor(item.portal, item.category))

        binding.tvDetailCategory.text = item.category
        updateStrengthBadge(item.passwordStrength)

        // Username
        binding.tvDetailUsername.text = item.username

        // Password masked/revealed
        updatePasswordView()

        // Notes
        if (decryptedNotes.isNotEmpty()) {
            binding.cardNotes.visibility = View.VISIBLE
            binding.tvDetailNotes.text = decryptedNotes
        } else {
            binding.cardNotes.visibility = View.GONE
        }
    }

    private fun updatePasswordView() {
        if (isPasswordRevealed) {
            binding.tvDetailPassword.text = decryptedPassword
            binding.btnDetailTogglePassword.setImageResource(R.drawable.ic_eye_off)
        } else {
            binding.tvDetailPassword.text = "••••••••••••"
            binding.btnDetailTogglePassword.setImageResource(R.drawable.ic_eye)
        }
    }

    private fun updateStrengthBadge(strength: String) {
        when (strength.uppercase(Locale.ROOT)) {
            "VERY_STRONG" -> {
                binding.tvDetailStrength.text = "Very Strong"
                binding.tvDetailStrength.setBackgroundResource(R.drawable.bg_pill_strength_strong)
                binding.tvDetailStrength.setTextColor(ContextCompat.getColor(this, R.color.brand_primary))
            }
            "STRONG" -> {
                binding.tvDetailStrength.text = "Strong"
                binding.tvDetailStrength.setBackgroundResource(R.drawable.bg_pill_strength_strong)
                binding.tvDetailStrength.setTextColor(ContextCompat.getColor(this, R.color.security_emerald))
            }
            "MEDIUM" -> {
                binding.tvDetailStrength.text = "Medium"
                binding.tvDetailStrength.setBackgroundResource(R.drawable.bg_pill_strength_medium)
                binding.tvDetailStrength.setTextColor(ContextCompat.getColor(this, R.color.security_amber))
            }
            else -> {
                binding.tvDetailStrength.text = "Weak"
                binding.tvDetailStrength.setBackgroundResource(R.drawable.bg_pill_strength_weak)
                binding.tvDetailStrength.setTextColor(ContextCompat.getColor(this, R.color.security_crimson))
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnOpenPortal.setOnClickListener {
            val portal = currentItem?.portal ?: return@setOnClickListener
            val url = if (portal.startsWith("http://") || portal.startsWith("https://")) {
                portal
            } else {
                "https://$portal"
            }
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open website: $url", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDetailCopyUsername.setOnClickListener {
            currentItem?.username?.let {
                ClipboardHelper.copyToClipboard(this, "Username", it, isSensitive = false)
            }
        }

        binding.btnDetailTogglePassword.setOnClickListener {
            isPasswordRevealed = !isPasswordRevealed
            updatePasswordView()
        }

        binding.btnDetailCopyPassword.setOnClickListener {
            ClipboardHelper.copyToClipboard(this, "Password", decryptedPassword, isSensitive = true)
        }

        binding.btnDetailCopyNotes.setOnClickListener {
            ClipboardHelper.copyToClipboard(this, "Sensitive Notes", decryptedNotes, isSensitive = true)
        }

        binding.btnEdit.setOnClickListener {
            val intent = Intent(this, AddEditCredentialActivity::class.java).apply {
                putExtra("item_id", itemId)
            }
            startActivity(intent)
        }

        binding.btnDelete.setOnClickListener {
            confirmDelete()
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(R.string.delete_confirm_message)
            .setPositiveButton(R.string.delete_credential) { _, _ ->
                dbHelper.delete(itemId)
                Toast.makeText(this, "Credential deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getPortalColor(portal: String, category: String): Int {
        val p = portal.lowercase(Locale.ROOT)
        return when {
            p.contains("facebook") -> Color.parseColor("#1877F2")
            p.contains("gmail") || p.contains("google") -> Color.parseColor("#EA4335")
            p.contains("github") -> Color.parseColor("#24292E")
            p.contains("netflix") -> Color.parseColor("#E50914")
            p.contains("hdfc") || p.contains("bank") -> Color.parseColor("#004C8F")
            p.contains("amazon") -> Color.parseColor("#FF9900")
            category == "Banking" -> Color.parseColor("#10B981")
            category == "Social" -> Color.parseColor("#3B82F6")
            category == "Work" -> Color.parseColor("#8B5CF6")
            else -> Color.parseColor("#6366F1")
        }
    }
}
