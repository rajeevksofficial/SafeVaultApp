package com.safevault.app

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.safevault.app.databinding.ActivityChangePinBinding

class ChangePinActivity : BaseSecureActivity() {

    enum class Stage {
        VERIFY_CURRENT,
        ENTER_NEW,
        CONFIRM_NEW
    }

    private lateinit var binding: ActivityChangePinBinding
    private val enteredDigits = StringBuilder()
    private var currentStage = Stage.VERIFY_CURRENT
    private var verifiedCurrentPin = ""
    private var newPinCandidate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateUiForStage()
        setupKeypadListeners()

        binding.btnBack.setOnClickListener { finish() }
        binding.tvCancel.setOnClickListener { finish() }
    }

    private fun updateUiForStage() {
        when (currentStage) {
            Stage.VERIFY_CURRENT -> {
                binding.tvStepBadge.text = getString(R.string.step_verify_current)
                binding.tvPromptTitle.text = getString(R.string.enter_current_pin_title)
                binding.tvPromptSubtitle.text = getString(R.string.enter_current_pin_desc)
            }
            Stage.ENTER_NEW -> {
                binding.tvStepBadge.text = getString(R.string.step_enter_new)
                binding.tvPromptTitle.text = getString(R.string.enter_new_pin_title)
                binding.tvPromptSubtitle.text = getString(R.string.enter_new_pin_desc)
            }
            Stage.CONFIRM_NEW -> {
                binding.tvStepBadge.text = getString(R.string.step_confirm_new)
                binding.tvPromptTitle.text = getString(R.string.confirm_new_pin_title)
                binding.tvPromptSubtitle.text = getString(R.string.confirm_new_pin_desc)
            }
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

        binding.btnKeyClear.setOnClickListener {
            if (enteredDigits.isNotEmpty()) {
                enteredDigits.clear()
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
        when (currentStage) {
            Stage.VERIFY_CURRENT -> {
                if (SecurityManager.verifyMasterPin(this, pin)) {
                    verifiedCurrentPin = pin
                    currentStage = Stage.ENTER_NEW
                    enteredDigits.clear()
                    updateDots()
                    updateUiForStage()
                    binding.tvError.visibility = View.INVISIBLE
                } else {
                    enteredDigits.clear()
                    updateDots()
                    binding.tvError.text = getString(R.string.pin_incorrect)
                    binding.tvError.visibility = View.VISIBLE
                }
            }

            Stage.ENTER_NEW -> {
                if (pin == verifiedCurrentPin) {
                    enteredDigits.clear()
                    updateDots()
                    binding.tvError.text = getString(R.string.pin_same_as_current)
                    binding.tvError.visibility = View.VISIBLE
                } else {
                    newPinCandidate = pin
                    currentStage = Stage.CONFIRM_NEW
                    enteredDigits.clear()
                    updateDots()
                    updateUiForStage()
                    binding.tvError.visibility = View.INVISIBLE
                }
            }

            Stage.CONFIRM_NEW -> {
                if (pin == newPinCandidate) {
                    val changed = SecurityManager.changeMasterPin(this, verifiedCurrentPin, pin)
                    if (changed) {
                        Toast.makeText(this, getString(R.string.pin_changed_success), Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        enteredDigits.clear()
                        updateDots()
                        binding.tvError.text = "Failed to update PIN. Please try again."
                        binding.tvError.visibility = View.VISIBLE
                    }
                } else {
                    // Mismatch, reset back to Stage.ENTER_NEW
                    currentStage = Stage.ENTER_NEW
                    newPinCandidate = ""
                    enteredDigits.clear()
                    updateDots()
                    updateUiForStage()
                    binding.tvError.text = getString(R.string.pin_mismatch)
                    binding.tvError.visibility = View.VISIBLE
                }
            }
        }
    }
}
