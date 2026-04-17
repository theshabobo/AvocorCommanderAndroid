package com.avocor.commander.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.avocor.commander.api.CommanderApi
import com.avocor.commander.api.LoginRequest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "avocor_prefs")

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_ROLE = stringPreferencesKey("role")
        private val KEY_REMEMBER = booleanPreferencesKey("remember_me")
        private val KEY_SAVED_USERNAME = stringPreferencesKey("saved_username")
        private val KEY_SAVED_PASSWORD = stringPreferencesKey("saved_password")
    }

    private val dataStore = application.dataStore

    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username

    private val _role = MutableStateFlow("")
    val role: StateFlow<String> = _role

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _savedUsername = MutableStateFlow("")
    val savedUsername: StateFlow<String> = _savedUsername

    private val _savedPassword = MutableStateFlow("")
    val savedPassword: StateFlow<String> = _savedPassword

    private val _rememberMe = MutableStateFlow(false)
    val rememberMe: StateFlow<Boolean> = _rememberMe

    val isLoggedIn: StateFlow<Boolean> = _token.map { it != null }.stateIn(
        viewModelScope, SharingStarted.Eagerly, false
    )

    init {
        viewModelScope.launch {
            dataStore.data.first().let { prefs ->
                _serverUrl.value = prefs[KEY_SERVER_URL] ?: "http://192.168.1.193:5105"
                _token.value = prefs[KEY_TOKEN]
                _username.value = prefs[KEY_USERNAME] ?: ""
                _role.value = prefs[KEY_ROLE] ?: ""
                _rememberMe.value = prefs[KEY_REMEMBER] ?: false
                _savedUsername.value = prefs[KEY_SAVED_USERNAME] ?: ""
                _savedPassword.value = prefs[KEY_SAVED_PASSWORD] ?: ""
            }
        }
    }

    fun login(serverUrl: String, username: String, password: String, remember: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val api = CommanderApi.create(serverUrl) { null }
                val response = api.login(LoginRequest(username, password))

                _serverUrl.value = serverUrl
                _token.value = response.token
                _username.value = response.username
                _role.value = response.role

                dataStore.edit { prefs ->
                    prefs[KEY_SERVER_URL] = serverUrl
                    prefs[KEY_TOKEN] = response.token
                    prefs[KEY_USERNAME] = response.username
                    prefs[KEY_ROLE] = response.role
                    prefs[KEY_REMEMBER] = remember
                    if (remember) {
                        prefs[KEY_SAVED_USERNAME] = username
                        prefs[KEY_SAVED_PASSWORD] = password
                    } else {
                        prefs.remove(KEY_SAVED_USERNAME)
                        prefs.remove(KEY_SAVED_PASSWORD)
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Login failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _token.value = null
            _username.value = ""
            _role.value = ""

            dataStore.edit { prefs ->
                prefs.remove(KEY_TOKEN)
                prefs.remove(KEY_USERNAME)
                prefs.remove(KEY_ROLE)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
