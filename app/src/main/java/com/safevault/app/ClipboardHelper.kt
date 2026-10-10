package com.safevault.app

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.widget.Toast

object ClipboardHelper {

    private val handler = Handler(Looper.getMainLooper())
    private var clearRunnable: Runnable? = null

    fun copyToClipboard(context: Context, label: String, text: String, isSensitive: Boolean = true) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = ClipData.newPlainText(label, text)

        if (isSensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
            clip.description.extras = extras
        }

        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied securely (auto-clears in 30s)", Toast.LENGTH_SHORT).show()

        // Cancel previous clear if any
        clearRunnable?.let { handler.removeCallbacks(it) }

        // Schedule auto-clear after 30 seconds
        val runnable = Runnable {
            try {
                if (clipboard.hasPrimaryClip()) {
                    val currentClip = clipboard.primaryClip
                    if (currentClip != null && currentClip.itemCount > 0) {
                        val currentText = currentClip.getItemAt(0).text?.toString()
                        if (currentText == text) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                clipboard.clearPrimaryClip()
                            } else {
                                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                            }
                        }
                    }
                }
            } catch (ignored: Exception) {
            }
        }
        clearRunnable = runnable
        handler.postDelayed(runnable, 30_000)
    }
}
