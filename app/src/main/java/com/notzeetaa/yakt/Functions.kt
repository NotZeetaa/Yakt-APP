package com.notzeetaa.yakt

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
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

class PreferencesManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("yakt_prefs", Context.MODE_PRIVATE)

    fun saveScriptVersion(version: String) {
        sharedPreferences.edit { putString("script_version", version) }
    }

    fun getScriptVersion(): String {
        return sharedPreferences.getString("script_version", "1.3") ?: "1.3"
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
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled) { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) MaterialTheme.colorScheme.surface else Color.Gray
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconId),
                contentDescription = null,
                tint = if (isEnabled) MaterialTheme.colorScheme.primary else Color.DarkGray
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = if (isEnabled) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f) else Color.DarkGray
                )
            }

            if (showSwitch && onSwitchChange != null) {
                Switch(
                    checked = switchState,
                    onCheckedChange = { if (isEnabled) onSwitchChange(it) },
                    enabled = isEnabled
                )
            }
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

fun executeShellScript(scriptFile: File) {
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
