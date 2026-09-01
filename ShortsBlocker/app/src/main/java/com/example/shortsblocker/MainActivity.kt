package com.example.shortsblocker

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val toggle = findViewById<Switch>(R.id.toggleSwitch)
        val statusText = findViewById<TextView>(R.id.statusText)
        val openSettingsButton = findViewById<Button>(R.id.openSettingsButton)

        fun refreshStatus() {
            val enabled = prefs.getBoolean(ShortsBlockerService.PREF_ENABLED, true)
            toggle.isChecked = enabled
            statusText.text = if (enabled) "Blocking: ON" else "Blocking: OFF"
        }

        refreshStatus()

        toggle.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(ShortsBlockerService.PREF_ENABLED, isChecked).apply()
            refreshStatus()
        }

        // Users must manually enable the Accessibility Service once — Android
        // does not allow apps to turn this on for themselves.
        openSettingsButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }
}
