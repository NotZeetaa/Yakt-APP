package com.notzeetaa.yakt

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

@SuppressLint("SdCardPath")
@Composable
fun HomePage(viewModel: MyViewModel, updateViewModel: UpdateViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val context = LocalContext.current
    val applyAtBootCheck by viewModel.applyAtBootCheck.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    var showModeDialog by remember { mutableStateOf(false) }
    var showRootDialog by remember { mutableStateOf(false) }
    var showLogDialog by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    var logContent by remember { mutableStateOf("") }
    val isRooted = isDeviceRooted()
    var showUpdateDialog by remember { mutableStateOf(false) }
    var scriptUpdateMessage by remember { mutableStateOf("") }
    val updateMessage by updateViewModel.updateMessage.collectAsState()
    val canUpdate by updateViewModel.canUpdate.collectAsState()
    val isUpdating by updateViewModel.isUpdating.collectAsState()
    val notFoundText = context.getString(R.string.log_file_not_found)

    val currentVersion by updateViewModel.currentVersion.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Card 1 - Run card
            ReusableCard(
                iconId = R.drawable.run_icon,
                title = stringResource(R.string.run_script),
                description = stringResource(R.string.run_script_description),
                isEnabled = isRooted,
                onClick = {
                    if (isRooted) {
                        showProgressDialog = true
                        val scriptName = when (selectedMode) {
                            0 -> "battery.sh"
                            1 -> "balanced.sh"
                            2 -> "gaming.sh"
                            3 -> "latency.sh"
                            else -> "balanced.sh"
                        }

                        // Modificado para usar o caminho correto
                        val scriptFile = File(context.filesDir, scriptName)

                        // Verifica se o arquivo existe
                        if (!scriptFile.exists()) {
                            // Se não existir, copia do assets
                            copyAssetFileToStorage(context, scriptName)
                        }

                        grantExecutePermission(scriptFile)
                        viewModel.viewModelScope.launch {
                            withContext(Dispatchers.IO) {
                                executeShellScript(scriptFile)
                            }
                            showProgressDialog = false
                            Toast.makeText(
                                context,
                                "Script execution completed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        showRootDialog = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Card 2 - Profile selection
            ReusableCard(
                iconId = R.drawable.profile_icon,
                title = stringResource(R.string.choose_mode),
                description = stringResource(
                    R.string.current_mode,
                    when (selectedMode) {
                        0 -> stringResource(R.string.mode_battery)
                        1 -> stringResource(R.string.mode_balanced)
                        2 -> stringResource(R.string.mode_gaming)
                        3 -> stringResource(R.string.mode_latency)
                        else -> stringResource(R.string.select_mode)
                    }
                ),
                isEnabled = isRooted,
                onClick = {
                    if (isRooted) {
                        showModeDialog = true
                    } else {
                        showRootDialog = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Card 3 - Card apply boot
            ReusableCard(
                iconId = R.drawable.apply_boot_icon,
                title = stringResource(R.string.apply_on_boot),
                description = stringResource(R.string.apply_on_boot_description),
                isEnabled = isRooted,
                showSwitch = true,
                switchState = applyAtBootCheck,
                onSwitchChange = {
                    if (isRooted) {
                        viewModel.saveApplyAtBoot(it)
                    } else {
                        showRootDialog = true
                    }
                },
                onClick = {
                    if (isRooted) {
                        viewModel.saveApplyAtBoot(!applyAtBootCheck)
                    } else {
                        showRootDialog = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Card 4 - Card log
            ReusableCard(
                iconId = R.drawable.log_icon,
                title = stringResource(R.string.view_log_title),
                description = stringResource(R.string.view_log_description),
                isEnabled = isRooted,
                onClick = {
                    if (isRooted) {
                        val logFile = File("/data/data/com.notzeetaa.yakt/files/yakt.log")
                        logContent = if (logFile.exists()) {
                            logFile.readText()
                        } else {
                            context.getString(R.string.log_file_not_found) // ✅ Fixed
                        }
                        showLogDialog = true
                    }
                }
            )
        }

        FloatingActionButton(
            onClick = {
                // Usa a versão do ViewModel e função atualizada
                updateViewModel.triggerUpdateCheck {
                    showUpdateDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Check for Updates")
        }

        if (showUpdateDialog && updateMessage != null) {
            GeneralDialog(
                title = stringResource(R.string.script_update_check_title),
                content = { Text(updateMessage!!) },
                onDismiss = { showUpdateDialog = false },
                confirmText = if (canUpdate) stringResource(R.string.update) else stringResource(R.string.ok),
                onConfirm = if (canUpdate) {
                    {
                        showUpdateDialog = false
                        updateViewModel.updateScripts(context) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else null
            )
        }

        if (isUpdating) {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Updating scripts, please wait...")
                    }
                },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            )
        }

        if (showRootDialog) {
            GeneralDialog(
                title = "Root Access Required",
                content = {
                    Text("This feature requires root access to function properly.")
                },
                onDismiss = { showRootDialog = false }
            )
        }

        if (showModeDialog) {
            GeneralDialog(
                title = stringResource(R.string.select_mode),
                content = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        listOf(
                            Pair(0, stringResource(R.string.mode_battery)),
                            Pair(1, stringResource(R.string.mode_balanced)),
                            Pair(2, stringResource(R.string.mode_gaming)),
                            Pair(3, stringResource(R.string.mode_latency))
                        ).forEach { (mode, modeText) ->
                            val isSelected = mode == selectedMode
                            Button(
                                onClick = {
                                    viewModel.saveSelectedMode(mode)
                                    showModeDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = if (isSelected) "$modeText (Selected)" else modeText)
                            }
                        }
                    }
                },
                onDismiss = { showModeDialog = false }
            )
        }

        if (showLogDialog) {
            GeneralDialog(
                title = "Script Log",
                content = {
                    Text(
                        text = logContent,
                        modifier = Modifier
                            .heightIn(min = 100.dp, max = 300.dp)
                            .verticalScroll(rememberScrollState())
                    )
                },
                onDismiss = { showLogDialog = false },
                confirmText = "Close",
                dismissText = "Clear Log",
                onDismissButton = {
                    viewModel.viewModelScope.launch {
                        withContext(Dispatchers.IO) {
                            try {
                                Runtime.getRuntime()
                                    .exec(
                                        arrayOf(
                                            "su",
                                            "-c",
                                            "rm /data/data/com.notzeetaa.yakt/files/yakt.log"
                                        )
                                    )
                                    .waitFor()
                                withContext(Dispatchers.Main) {
                                    logContent =
                                        if (File("/data/data/com.notzeetaa.yakt/files/yakt.log").exists()) {
                                            "Failed to clear log!"
                                        } else {
                                            "Log cleared successfully!"
                                        }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    logContent = "Error: ${e.message}"
                                }
                            }
                        }
                    }
                },
                width = 350.dp
            )
        }

        if (showProgressDialog) {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Executing script, please wait...")
                    }
                },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            )
        }
    }
}


