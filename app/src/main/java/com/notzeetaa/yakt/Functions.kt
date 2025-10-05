package com.notzeetaa.yakt

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import androidx.core.content.edit
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class PreferencesManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("yakt_prefs", Context.MODE_PRIVATE)

    fun saveScriptVersion(version: String) {
        sharedPreferences.edit { putString("script_version", version) }
    }

    fun getScriptVersion(): String {
        return sharedPreferences.getString("script_version", "1.4") ?: "1.4"
    }
}

class MyViewModel(private val dataStoreManager: DataStoreManager) : ViewModel() {
    var showDexApplyButton by mutableStateOf(false)
    var selectedDexOption by mutableStateOf<String?>(null)

    val applyAtBootCheck = dataStoreManager.applyAtBootFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = false
    )

    val selectedMode = dataStoreManager.selectedModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = 0
    )

    val onePlusBootCheck = dataStoreManager.onePlusFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = false
    )

    val framerateCheck = dataStoreManager.framerateFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = false
    )

    fun saveApplyAtBoot(value: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveApplyAtBoot(value)
        }
    }

    fun saveSelectedMode(mode: Int) {
        viewModelScope.launch {
            dataStoreManager.saveSelectedMode(mode)
        }
    }

    fun saveOnePlusBoot(value: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveOnePlusBoot(value)
        }
    }

    fun saveframerateBoot(value: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveframerateBoot(value)
        }
    }

    fun selectDexOption(option: String) {
        selectedDexOption = option
        showDexApplyButton = true
    }

    fun clearDexSelection() {
        selectedDexOption = null
        showDexApplyButton = false
    }
}

@Composable
fun GeneralDialog(
    title: String,
    content: @Composable () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "OK",
    onConfirm: (() -> Unit)? = null,
    dismissText: String? = null,
    onDismissButton: (() -> Unit)? = null,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    width: Dp = Dp.Unspecified
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm?.invoke() ?: onDismiss() }) {
                Text(confirmText)
            }
        },
        dismissButton = dismissText?.let {
            {
                TextButton(onClick = { onDismissButton?.invoke() ?: onDismiss() }) {
                    Text(it)
                }
            }
        },
        modifier = if (width != Dp.Unspecified) Modifier.width(width) else Modifier,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside
        )
    )
}

@Composable
fun ReusableCard(
    iconId: Int,
    title: String,
    description: String,
    isEnabled: Boolean,
    showSwitch: Boolean = false,
    switchState: Boolean = false,
    onSwitchChange: ((Boolean) -> Unit)? = null,
    onClick: () -> Unit,
    // 👇 NEW slot for custom UI on the right side
    extraContent: @Composable RowScope.() -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = isEnabled,
                onClick = onClick,
                indication = null, // Fix for Material 3 compatibility
                interactionSource = remember { MutableInteractionSource() }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconId),
                contentDescription = null,
                tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            if (showSwitch && onSwitchChange != null) {
                Switch(
                    checked = switchState,
                    onCheckedChange = { if (isEnabled) onSwitchChange(it) },
                    enabled = isEnabled
                )
            }
            // 👇 Place for extra content (Stop AI button, etc.)
            extraContent()
        }
    }
}

// Adicione estas funções em um objeto Utils ou como funções de topo
fun copyAssetFileToStorage(context: Context, fileName: String): File {
    val targetFile = File(context.filesDir, fileName)

    if (!targetFile.exists()) {
        context.assets.open(fileName).use { inputStream ->
            FileOutputStream(targetFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    return targetFile
}

fun grantExecutePermission(file: File) {
    file.setExecutable(true, false)
}

suspend fun executeShellScript(scriptFile: File, selectedMode: String) {
    try {
        // Build absolute path
        val scriptPath = scriptFile.absolutePath

        // Make sure it's executable
        scriptFile.setExecutable(true)

        Log.d(TAG, "executeShellScript: $selectedMode")

        // Run with su -c "sh /path/to/script"
        val process = Runtime.getRuntime().exec(
            arrayOf("su", "-c", "sh $scriptPath", selectedMode)
        )

        stopAiScript()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val error = process.errorStream.bufferedReader().readText()
            println("Script failed: $error")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun executeShellScriptAI(scriptFile: File) {
    try {
        Runtime.getRuntime().exec(arrayOf("su", "-c", scriptFile.absolutePath)).waitFor()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun clearAndCreateCacheFolder(context: Context) {
    val cacheDir = context.cacheDir
    if (cacheDir.exists()) {
        cacheDir.deleteRecursively()
    }
    cacheDir.mkdirs()
}

suspend fun isAiRunning(): Boolean = withContext(Dispatchers.IO) {
    return@withContext try {
        // Try multiple approaches to detect the process
        val commands = arrayOf(
            arrayOf("su", "-c", "ps -A | grep ai.sh"),
            arrayOf("su", "-c", "pgrep -f ai.sh"),
            arrayOf("su", "-c", "pidof ai.sh")
        )

        for (cmd in commands) {
            try {
                val process = Runtime.getRuntime().exec(cmd)
                val output = process.inputStream.bufferedReader().readText().trim()
                val exitCode = process.waitFor()

                if (exitCode == 0 && output.isNotBlank()) {
                    return@withContext true
                }
            } catch (e: Exception) {
                // Try next command if this one fails
                continue
            }
        }
        false
    } catch (e: Exception) {
        false
    }
}

// Logging utility function
private fun logToFile(message: String) {
    try {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val logMessage = "[$timestamp] [USER_ACTION] $message\n"

        // Append to log file with su privileges
        Runtime.getRuntime().exec(arrayOf("su", "-c", "echo '$logMessage' >> /data/data/com.notzeetaa.yakt/files/yakt.log"))
    } catch (e: Exception) {
        Log.e("LOG_TO_FILE", "Failed to write to log file: ${e.message}")
    }
}

suspend fun stopAiScript(): Boolean = withContext(Dispatchers.IO) {
    return@withContext try {
        // Method 2: Use pkill if available (more reliable)
        val pkillProcess = Runtime.getRuntime().exec(arrayOf("su", "-c", "pkill -f 'ai.sh'"))
        val exitCode = pkillProcess.waitFor()

        if (exitCode == 0) {
            Log.d("STOP_AI", "pkill successfully stopped AI script")
            // Log to yakt.log file
            logToFile("AI script was stopped by the user via pkill")
            true
        } else {
            // Fallback to pgrep + kill method
            Log.d("STOP_AI", "pkill failed, trying fallback method")
            stopAiScriptFallback()
        }
    } catch (e: Exception) {
        Log.e("STOP_AI", "pkill failed, trying fallback: ${e.message}")
        stopAiScriptFallback()
    }
}

private suspend fun stopAiScriptFallback(): Boolean = withContext(Dispatchers.IO) {
    try {
        // Find all processes containing ai.sh
        val psProcess = Runtime.getRuntime().exec(arrayOf("su", "-c", "ps -ef | grep 'ai.sh' | grep -v grep"))
        val output = psProcess.inputStream.bufferedReader().readText().trim()
        psProcess.waitFor()

        if (output.isNotEmpty()) {
            val lines = output.split("\n")
            var success = true
            var stoppedProcesses = mutableListOf<String>()

            for (line in lines) {
                try {
                    // Extract PID - usually the second column in ps output
                    val parts = line.trim().split(Regex("\\s+"))
                    if (parts.size >= 2) {
                        val pid = parts[1]
                        val killProcess = Runtime.getRuntime().exec(arrayOf("su", "-c", "kill -9 $pid"))
                        val exitCode = killProcess.waitFor()
                        if (exitCode == 0) {
                            stoppedProcesses.add(pid)
                        } else {
                            success = false
                            Log.e("STOP_AI", "Failed to kill process $pid")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("STOP_AI", "Error processing line: $line - ${e.message}")
                    success = false
                }
            }

            // Log the result
            if (stoppedProcesses.isNotEmpty()) {
                logToFile("AI script was stopped by the user (PIDs: ${stoppedProcesses.joinToString(", ")})")
            } else if (!success) {
                logToFile("Failed to stop AI script - some processes may still be running")
            }

            success
        } else {
            logToFile("No running AI script found to stop")
            false
        }
    } catch (e: Exception) {
        Log.e("STOP_AI", "Fallback method failed: ${e.message}")
        logToFile("Error stopping AI script: ${e.message}")
        false
    }
}