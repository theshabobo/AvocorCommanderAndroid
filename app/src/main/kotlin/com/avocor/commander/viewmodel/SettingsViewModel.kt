package com.avocor.commander.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.MessageDigest

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "avocor_settings")

enum class AppMode { PHONE, TABLET }

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private val KEY_APP_MODE = stringPreferencesKey("app_mode")
        private val KEY_TABLET_ROOM_ID = intPreferencesKey("tablet_room_id")
        private val KEY_TABLET_ROOM_NAME = stringPreferencesKey("tablet_room_name")
        private val KEY_ADMIN_PIN_HASH = stringPreferencesKey("admin_pin_hash")
        private val KEY_KIOSK_ENABLED = booleanPreferencesKey("kiosk_enabled")

        private fun hashPin(pin: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }

    private val dataStore = application.settingsStore

    private val _appMode = MutableStateFlow(AppMode.PHONE)
    val appMode: StateFlow<AppMode> = _appMode

    private val _assignedRoomId = MutableStateFlow<Int?>(null)
    val assignedRoomId: StateFlow<Int?> = _assignedRoomId

    private val _assignedRoomName = MutableStateFlow("")
    val assignedRoomName: StateFlow<String> = _assignedRoomName

    private val _kioskEnabled = MutableStateFlow(false)
    val kioskEnabled: StateFlow<Boolean> = _kioskEnabled

    private val _hasPinConfigured = MutableStateFlow(false)
    val hasPinConfigured: StateFlow<Boolean> = _hasPinConfigured

    // True once DataStore has been read — prevents flash of wrong UI
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private var pinHash: String = ""

    init {
        viewModelScope.launch {
            dataStore.data.first().let { prefs ->
                _appMode.value = when (prefs[KEY_APP_MODE]) {
                    "tablet" -> AppMode.TABLET
                    else -> AppMode.PHONE
                }
                _assignedRoomId.value = prefs[KEY_TABLET_ROOM_ID]
                _assignedRoomName.value = prefs[KEY_TABLET_ROOM_NAME] ?: ""
                _kioskEnabled.value = prefs[KEY_KIOSK_ENABLED] ?: false
                pinHash = prefs[KEY_ADMIN_PIN_HASH] ?: ""
                _hasPinConfigured.value = pinHash.isNotEmpty()
                _isReady.value = true
            }
        }
    }

    fun setMode(mode: AppMode) {
        _appMode.value = mode
        viewModelScope.launch {
            dataStore.edit { it[KEY_APP_MODE] = if (mode == AppMode.TABLET) "tablet" else "phone" }
        }
    }

    fun assignRoom(groupId: Int, groupName: String) {
        _assignedRoomId.value = groupId
        _assignedRoomName.value = groupName
        viewModelScope.launch {
            dataStore.edit {
                it[KEY_TABLET_ROOM_ID] = groupId
                it[KEY_TABLET_ROOM_NAME] = groupName
            }
        }
    }

    fun setPin(pin: String) {
        pinHash = hashPin(pin)
        _hasPinConfigured.value = true
        viewModelScope.launch {
            dataStore.edit { it[KEY_ADMIN_PIN_HASH] = pinHash }
        }
    }

    fun verifyPin(pin: String): Boolean {
        return pinHash.isNotEmpty() && hashPin(pin) == pinHash
    }

    fun setKioskEnabled(enabled: Boolean) {
        _kioskEnabled.value = enabled
        viewModelScope.launch {
            dataStore.edit { it[KEY_KIOSK_ENABLED] = enabled }
        }
        // Also write to SharedPreferences for fast read by MainActivity/BootReceiver
        getApplication<Application>().getSharedPreferences("kiosk_state", android.content.Context.MODE_PRIVATE)
            .edit().putBoolean("kiosk_enabled", enabled).apply()
    }
}
