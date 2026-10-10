package com.safevault.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.safevault.app.databinding.ActivityAuthBinding

class AuthActivity : BaseSecureActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val enteredDigits = StringBuilder()
    private var isSettingUp = false
    private var tempPin = ""
    private var isConfirming = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isSettingUp = !SecurityManager.isMasterPinSet(this)
        updateHeaderUi()
        setupKeypadListeners()

        binding.tvResetVault.setOnClickListener {
            showResetVaultDialog()
        }

        binding.btnBiometric.setOnClickListener {
            Toast.makeText(this, "Master PIN authentication is active", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateHeaderUi() {
        if (isSettingUp) {
            if (!isConfirming) {
                binding.tvPromptTitle.text = getString(R.string.create_pin)
                binding.tvPromptSubtitle.text = getString(R.string.create_pin_subtitle)
            } else {
                binding.tvPromptTitle.text = getString(R.string.confirm_pin)
                binding.tvPromptSubtitle.text = getString(R.string.confirm_pin_subtitle)
            }
            binding.tvResetVault.visibility = View.GONE
        } else {
            binding.tvPromptTitle.text = getString(R.string.unlock_vault)
            binding.tvPromptSubtitle.text = getString(R.string.enter_pin)
            binding.tvResetVault.visibility = View.VISIBLE
        }
    }

    private fun setupKeypadListeners() {
        val numberButtons = listOf(
            binding.btnKey0, binding.btnKey1, binding.btnKey2, binding.btnKey3,
            binding.btnKey4, binding.btnKey5, binding.btnKey6, binding.btnKey7,
            binding.btnKey8, binding.btnKey9
        )

        numberButtons.forEachIndexed { index, button ->
            button.setOnClickListener { onDigitPressed(index.toString()) }
        }

        binding.btnBackspace.setOnClickListener {
            if (enteredDigits.isNotEmpty()) {
                enteredDigits.deleteCharAt(enteredDigits.length - 1)
                updateDots()
                binding.tvError.visibility = View.INVISIBLE
            }
        }
    }

    private fun onDigitPressed(digit: String) {
        if (enteredDigits.length < 4) {
            enteredDigits.append(digit)
            updateDots()
            binding.tvError.visibility = View.INVISIBLE

            if (enteredDigits.length == 4) {
                handlePinCompleted(enteredDigits.toString())
            }
        }
    }

    private fun updateDots() {
        val dots = listOf(binding.ivDot1, binding.ivDot2, binding.ivDot3, binding.ivDot4)
        for (i in dots.indices) {
            if (i < enteredDigits.length) {
                dots[i].setImageResource(R.drawable.bg_pin_dot_filled)
            } else {
                dots[i].setImageResource(R.drawable.bg_pin_dot_empty)
            }
        }
    }

    private fun handlePinCompleted(pin: String) {
        if (isSettingUp) {
            if (!isConfirming) {
                tempPin = pin
                isConfirming = true
                enteredDigits.clear()
                updateDots()
                updateHeaderUi()
            } else {
                if (pin == tempPin) {
                    SecurityManager.setMasterPin(this, pin)
                    VaultDatabaseHelper(this).seedStarterDataIfNeeded()
                    Toast.makeText(this, "Master PIN created successfully", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    isConfirming = false
                    tempPin = ""
                    enteredDigits.clear()
                    updateDots()
                    updateHeaderUi()
                    binding.tvError.text = getString(R.string.pin_mismatch)
                    binding.tvError.visibility = View.VISIBLE
                }
            }
        } else {
            if (SecurityManager.verifyMasterPin(this, pin)) {
                navigateToMain()
            } else {
                enteredDigits.clear()
                updateDots()
                binding.tvError.text = getString(R.string.pin_incorrect)
                binding.tvError.visibility = View.VISIBLE
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showResetVaultDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reset SafeVault?")
            .setMessage("For security, resetting your Master PIN will wipe all encrypted passwords and notes. This cannot be undone.")
            .setPositiveButton("Wipe & Reset") { _, _ ->
                SecurityManager.resetVault(this)
                VaultDatabaseHelper(this).deleteAll()
                isSettingUp = true
                isConfirming = false
                tempPin = ""
                enteredDigits.clear()
                updateDots()
                updateHeaderUi()
                Toast.makeText(this, "Vault reset. Please set a new Master PIN.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
