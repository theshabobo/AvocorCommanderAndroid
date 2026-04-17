package com.avocor.commander

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.avocor.commander.kiosk.KioskManager
import com.avocor.commander.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check kiosk mode on launch via shared preferences (lightweight read)
        val prefs = getSharedPreferences("kiosk_state", MODE_PRIVATE)
        if (prefs.getBoolean("kiosk_enabled", false)) {
            KioskManager.enableKiosk(this)
        }

        setContent {
            AvocorApp()
        }
    }
}
