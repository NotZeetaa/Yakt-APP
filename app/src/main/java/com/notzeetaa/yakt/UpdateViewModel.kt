package com.notzeetaa.yakt

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.serialization.json.Json
import java.io.File

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val preferencesManager = PreferencesManager(context)

    private val _updateAvailable = MutableStateFlow(false)
    val updateAvailable: StateFlow<Boolean> = _updateAvailable

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private val _updateMessage = MutableStateFlow<String?>(null)
    val updateMessage: StateFlow<String?> = _updateMessage

    private val _canUpdate = MutableStateFlow(false)
    val canUpdate: StateFlow<Boolean> = _canUpdate

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating

    private val _currentVersion = MutableStateFlow(preferencesManager.getScriptVersion())
    val currentVersion: StateFlow<String> = _currentVersion

    private var latestVersion: String? = null
    private var scriptsToUpdate: List<String> = emptyList()

    private val requiredScripts = listOf(
        "battery.sh",
        "balanced.sh",
        "extras.sh",
        "gaming.sh",
        "latency.sh"
    )

    suspend fun checkForUpdate() {
        try {
            val url = "https://raw.githubusercontent.com/NotZeetaa/yakt-scripts/refs/heads/main/version.json"
            val response = withContext(Dispatchers.IO) {
                fetchRemoteJson(url)
            }
            latestVersion = response.version

            val hasUpdate = _currentVersion.value < response.version
            _updateAvailable.value = hasUpdate
            scriptsToUpdate = requiredScripts

            if (hasUpdate) {
                _canUpdate.value = true
                val changelogText = response.changelog.joinToString("\n• ", prefix = "• ")
                _updateMessage.value = context.getString(R.string.update_available_title, response.version) +
                        "\n\n" + changelogText
            } else {
                _canUpdate.value = false
                _updateMessage.value = context.getString(R.string.update_latest_version, _currentVersion.value)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _canUpdate.value = false
            _updateMessage.value = context.getString(
                R.string.update_check_failed,
                e.localizedMessage ?: e.toString()
            )
        }
    }

    private suspend fun fetchRemoteJson(url: String): RemoteVersionResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception(context.getString(R.string.http_error, response.code))
            val body = response.body?.string() ?: throw Exception(context.getString(R.string.empty_response))
            return@withContext json.decodeFromString<RemoteVersionResponse>(body)
        }
    }

    fun updateScripts(context: Context, onComplete: (success: Boolean, msg: String) -> Unit) {
        viewModelScope.launch {
            _isUpdating.value = true
            try {
                withContext(Dispatchers.IO) {
                    downloadAndSaveScripts(context, scriptsToUpdate)
                }

                latestVersion?.let {
                    preferencesManager.saveScriptVersion(it)
                    _currentVersion.value = it
                }

                _isUpdating.value = false
                onComplete(true, context.getString(R.string.update_success, latestVersion))
                _updateAvailable.value = false
            } catch (e: Exception) {
                _isUpdating.value = false
                onComplete(false, context.getString(R.string.update_failed, e.message))
            }
        }
    }

    private fun downloadAndSaveScripts(context: Context, scriptsList: List<String>) {
        val baseUrl = "https://raw.githubusercontent.com/NotZeetaa/yakt-scripts/main/"
        scriptsList.forEach { script ->
            val url = baseUrl + script
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful)
                    throw Exception(context.getString(R.string.download_failed, script))
                val content = response.body?.string()
                    ?: throw Exception(context.getString(R.string.empty_script_content))
                val file = File(context.filesDir, script)
                file.writeText(content)
                file.setExecutable(true)
            }
        }
    }

    fun triggerUpdateCheck(onDone: () -> Unit) {
        viewModelScope.launch {
            checkForUpdate()
            onDone()
        }
    }

    fun resetUpdateNotification() {
        _updateAvailable.value = false
    }
}
