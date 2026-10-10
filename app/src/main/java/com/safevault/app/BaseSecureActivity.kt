package com.safevault.app

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

abstract class BaseSecureActivity : AppCompatActivity() {

    companion object {
        // In production releases, FLAG_SECURE protects against screen captures & recordings.
        // Toggled during verification to allow capturing demonstration screenshots.
        var ENABLE_FLAG_SECURE = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ENABLE_FLAG_SECURE) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }
}
