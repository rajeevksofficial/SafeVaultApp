package com.safevault.app

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.safevault.app.databinding.ActivityAddEditCredentialBinding

class AddEditCredentialActivity : BaseSecureActivity() {

    private lateinit var binding: ActivityAddEditCredentialBinding
    private lateinit var dbHelper: VaultDatabaseHelper

    private var editingItemId: Long = -1L
    private var existingItem: VaultItem? = null
    private var isPasswordVisible = false

    private val categories = listOf("Social", "Email", "Banking", "Work", "Shopping", "Other")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditCredentialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = VaultDatabaseHelper(this)
        editingItemId = intent.getLongExtra("item_id", -1L)

        setupCategorySpinner()
        setupPresetChips()
        setupPasswordGenerator()
        setupPasswordWatcher()
        setupListeners()

        if (editingItemId != -1L) {
            loadExistingItem(editingItemId)
        } else {
            binding.tvHeaderTitle.text = getString(R.string.add_credential)
            regeneratePassword()
        }
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        binding.spCategory.adapter = adapter
    }

    private fun setupPresetChips() {
        binding.chipGmail.setOnClickListener {
            binding.etPortal.setText("gmail.com")
            if (binding.etTitle.text.isNullOrEmpty()) binding.etTitle.setText("Google / Gmail Account")
            binding.spCategory.setSelection(categories.indexOf("Email"))
        }

        binding.chipFacebook.setOnClickListener {
            binding.etPortal.setText("facebook.com")
            if (binding.etTitle.text.isNullOrEmpty()) binding.etTitle.setText("Facebook Account")
            binding.spCategory.setSelection(categories.indexOf("Social"))
        }

        binding.chipInstagram.setOnClickListener {
            binding.etPortal.setText("instagram.com")
            if (binding.etTitle.text.isNullOrEmpty()) binding.etTitle.setText("Instagram Profile")
            binding.spCategory.setSelection(categories.indexOf("Social"))
        }

        binding.chipGitHub.setOnClickListener {
            binding.etPortal.setText("github.com")
            if (binding.etTitle.text.isNullOrEmpty()) binding.etTitle.setText("GitHub Account")
            binding.spCategory.setSelection(categories.indexOf("Work"))
        }

        binding.chipTwitter.setOnClickListener {
            binding.etPortal.setText("x.com")
            if (binding.etTitle.text.isNullOrEmpty()) binding.etTitle.setText("X / Twitter")
            binding.spCategory.setSelection(categories.indexOf("Social"))
        }

        binding.chipNetflix.setOnClickListener {
            binding.etPortal.setText("netflix.com")
            if (binding.etTitle.text.isNullOrEmpty()) binding.etTitle.setText("Netflix Streaming")
            binding.spCategory.setSelection(categories.indexOf("Other"))
        }

        binding.chipAmazon.setOnClickListener {
            binding.etPortal.setText("amazon.com")
            if (binding.etTitle.text.isNullOrEmpty()) binding.etTitle.setText("Amazon Shopping")
            binding.spCategory.setSelection(categories.indexOf("Shopping"))
        }

        binding.chipBanking.setOnClickListener {
            binding.etPortal.setText("banking-portal.com")
            if (binding.etTitle.text.isNullOrEmpty()) binding.etTitle.setText("NetBanking Portal")
            binding.spCategory.setSelection(categories.indexOf("Banking"))
        }
    }

    private fun setupPasswordGenerator() {
        binding.sbLength.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val len = progress + 8
                binding.tvLengthValue.text = "$len chars"
                regeneratePassword()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val toggleListener = View.OnClickListener { regeneratePassword() }
        binding.cbUpper.setOnClickListener(toggleListener)
        binding.cbLower.setOnClickListener(toggleListener)
        binding.cbDigits.setOnClickListener(toggleListener)
        binding.cbSymbols.setOnClickListener(toggleListener)

        binding.btnRegenerate.setOnClickListener { regeneratePassword() }

        binding.btnApplyGenerated.setOnClickListener {
            val generated = binding.tvGeneratedPreview.text.toString()
            binding.etPassword.setText(generated)
            Toast.makeText(this, "Generated password applied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun regeneratePassword() {
        val len = binding.sbLength.progress + 8
        val pass = PasswordUtils.generatePassword(
            length = len,
            includeUpper = binding.cbUpper.isChecked,
            includeLower = binding.cbLower.isChecked,
            includeDigits = binding.cbDigits.isChecked,
            includeSymbols = binding.cbSymbols.isChecked
        )
        binding.tvGeneratedPreview.text = pass
    }

    private fun setupPasswordWatcher() {
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateStrengthMeter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateStrengthMeter(password: String) {
        val result = PasswordUtils.evaluateStrength(password)
        binding.pbStrength.progress = result.scorePercent

        val color = ContextCompat.getColor(this, result.level.colorRes)
        binding.pbStrength.progressTintList = ColorStateList.valueOf(color)
        binding.tvStrengthLabel.text = result.level.label
        binding.tvStrengthLabel.setTextColor(color)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnTogglePasswordVisibility.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                binding.btnTogglePasswordVisibility.setImageResource(R.drawable.ic_eye_off)
            } else {
                binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.btnTogglePasswordVisibility.setImageResource(R.drawable.ic_eye)
            }
            binding.etPassword.setSelection(binding.etPassword.text.length)
        }

        binding.btnSave.setOnClickListener {
            saveCredential()
        }

        binding.btnDelete.setOnClickListener {
            confirmDelete()
        }
    }

    private fun loadExistingItem(id: Long) {
        val item = dbHelper.getItemById(id) ?: run {
            Toast.makeText(this, "Credential not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        existingItem = item

        binding.tvHeaderTitle.text = getString(R.string.edit_credential)
        binding.etTitle.setText(item.title)
        binding.etPortal.setText(item.portal)
        binding.etUsername.setText(item.username)

        val decryptedPass = SecurityManager.decrypt(item.passwordEncrypted, item.passwordIv)
        binding.etPassword.setText(decryptedPass)

        if (item.notesEncrypted.isNotEmpty()) {
            val decryptedNotes = SecurityManager.decrypt(item.notesEncrypted, item.notesIv)
            binding.etNotes.setText(decryptedNotes)
        }

        val catIndex = categories.indexOf(item.category)
        if (catIndex != -1) {
            binding.spCategory.setSelection(catIndex)
        }

        binding.btnDelete.visibility = View.VISIBLE
    }

    private fun saveCredential() {
        val title = binding.etTitle.text.toString().trim()
        val portal = binding.etPortal.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val notes = binding.etNotes.text.toString().trim()
        val category = binding.spCategory.selectedItem?.toString() ?: "Other"

        if (title.isEmpty()) {
            binding.etTitle.error = "Please enter a title"
            binding.etTitle.requestFocus()
            return
        }

        if (portal.isEmpty()) {
            binding.etPortal.error = "Please enter portal / website"
            binding.etPortal.requestFocus()
            return
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Please enter or generate a password"
            binding.etPassword.requestFocus()
            return
        }

        val passEncryption = SecurityManager.encrypt(password)
        val notesEncryption = if (notes.isNotEmpty()) SecurityManager.encrypt(notes) else Pair("", "")
        val strength = PasswordUtils.evaluateStrength(password).level.name

        if (existingItem != null) {
            val updated = existingItem!!.copy(
                title = title,
                portal = portal,
                username = username,
                passwordEncrypted = passEncryption.first,
                passwordIv = passEncryption.second,
                category = category,
                notesEncrypted = notesEncryption.first,
                notesIv = notesEncryption.second,
                passwordStrength = strength
            )
            dbHelper.update(updated)
            Toast.makeText(this, "Credential updated securely", Toast.LENGTH_SHORT).show()
        } else {
            val newItem = VaultItem(
                title = title,
                portal = portal,
                username = username,
                passwordEncrypted = passEncryption.first,
                passwordIv = passEncryption.second,
                category = category,
                notesEncrypted = notesEncryption.first,
                notesIv = notesEncryption.second,
                passwordStrength = strength
            )
            dbHelper.insert(newItem)
            Toast.makeText(this, "Credential saved securely", Toast.LENGTH_SHORT).show()
        }

        finish()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(R.string.delete_confirm_message)
            .setPositiveButton(R.string.delete_credential) { _, _ ->
                if (existingItem != null) {
                    dbHelper.delete(existingItem!!.id)
                    Toast.makeText(this, "Credential deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
