package com.notzeetaa.yakt

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.withContext
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

// Add this enum for navigation
enum class AppScreen {
    HOME,
    LOG_VIEWER
}

@SuppressLint("SdCardPath")
@Composable
fun HomePage(
    viewModel: MyViewModel,
    updateViewModel: UpdateViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val applyAtBootCheck by viewModel.applyAtBootCheck.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    var showModeDialog by remember { mutableStateOf(false) }
    var showRootDialog by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    val isRooted = isDeviceRooted()
    var showUpdateDialog by remember { mutableStateOf(false) }
    val updateMessage by updateViewModel.updateMessage.collectAsState()
    val canUpdate by updateViewModel.canUpdate.collectAsState()
    val isUpdating by updateViewModel.isUpdating.collectAsState()
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
                            0 -> "yakt.sh"
                            1 -> "yakt.sh"
                            2 -> "yakt.sh"
                            3 -> "yakt.sh"
                            4 -> "ai.sh"
                            else -> "yakt.sh"
                        }

                        val currentMode = when (selectedMode) {
                            0 -> "battery"
                            1 -> "balanced"
                            2 -> "gaming"
                            3 -> "latency"
                            else -> "balanced"
                        }

                        val scriptFile = File(context.filesDir, scriptName)
                        if (!scriptFile.exists()) {
                            copyAssetFileToStorage(context, scriptName)
                        }

                        grantExecutePermission(scriptFile)
                        viewModel.viewModelScope.launch {
                            withContext(Dispatchers.IO) {
                                if (scriptName != "ai.sh") {
                                    executeShellScript(scriptFile, currentMode)
                                } else {
                                    showProgressDialog = false
                                    executeShellScriptAI(scriptFile)
                                }
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
                },
                // 👇 Extra button: only visible in AI mode
                extraContent = {
                    var aiRunning by remember { mutableStateOf(false) }

                    // Keep checking every 2 seconds
                    LaunchedEffect(Unit) {
                        while (true) {
                            aiRunning = isAiRunning()
                            kotlinx.coroutines.delay(2000)
                        }
                    }

                    if (aiRunning && isRooted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.viewModelScope.launch {
                                    val success = stopAiScript()
                                    aiRunning = isAiRunning() // refresh after stopping
                                    Toast.makeText(
                                        context,
                                        if (success) "AI script stopped!" else "Failed to stop AI script",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Stop AI")
                        }
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
                        4 -> stringResource(R.string.mode_ai)
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

            // Card 4 - Card log (now navigates to LogViewerPage)
            ReusableCard(
                iconId = R.drawable.log_icon,
                title = stringResource(R.string.view_log_title),
                description = stringResource(R.string.view_log_description),
                isEnabled = isRooted,
                onClick = {
                    if (isRooted) {
                        // Navigate to log viewer page
                        navController.navigate(AppScreen.LOG_VIEWER.name)
                    } else {
                        showRootDialog = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Card 5 - Donation card
            ReusableCard(
                iconId = R.drawable.donate_icon,
                title = stringResource(R.string.donation_title),
                description = stringResource(R.string.donation_description),
                isEnabled = true,
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://notzeetaa.github.io/Donate-NotZeetaa/"))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Cannot open browser: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }

        FloatingActionButton(
            onClick = {
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
                            Pair(3, stringResource(R.string.mode_latency)),
                            Pair(4, stringResource(R.string.mode_ai))
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

// New Log Viewer Page
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LogViewerPage(
    navController: NavHostController,
    viewModel: MyViewModel
) {
    val context = LocalContext.current
    var logContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val logFile = File("/data/data/com.notzeetaa.yakt/files/yakt.log")
        logContent = if (logFile.exists()) {
            logFile.readText()
        } else {
            context.getString(R.string.log_file_not_found)
        }
        isLoading = false
    }

    AnimatedContent(
        targetState = isLoading,
        transitionSpec = {
            if (targetState) {
                fadeIn(tween(300)) with fadeOut(tween(300))
            } else {
                slideInVertically(tween(500)) { it } with slideOutVertically(tween(500)) { -it }
            }
        }
    ) { loading ->
        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 3.dp
                )
            }
        } else {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = "Script Log",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { navController.popBackStack() }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showClearDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear Log"
                                )
                            }
                        }
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { showClearDialog = true },
                        icon = { Icon(Icons.Default.Delete, contentDescription = "Clear") },
                        text = { Text("Clear Log") }
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    if (logContent.isNotEmpty()) {
                        val logLines = logContent.split("\n")

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .padding(16.dp)
                        ) {
                            items(logLines) { line ->
                                AnimatedVisibility(
                                    visible = line.isNotEmpty(),
                                    enter = fadeIn(tween(300)) + slideInVertically(tween(300)),
                                    exit = fadeOut(tween(150))
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        elevation = CardDefaults.cardElevation(2.dp)
                                    ) {
                                        Text(
                                            text = line,
                                            modifier = Modifier.padding(12.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            overflow = TextOverflow.Ellipsis,
                                            maxLines = 3
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "${logLines.size} log entries",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No log entries found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Log") },
            text = { Text("Are you sure you want to clear all log entries?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.viewModelScope.launch {
                            withContext(Dispatchers.IO) {
                                try {
                                    Runtime.getRuntime()
                                        .exec(arrayOf("su", "-c", "rm /data/data/com.notzeetaa.yakt/files/yakt.log"))
                                        .waitFor()
                                    logContent = "Log cleared successfully!"
                                } catch (e: Exception) {
                                    logContent = "Error: ${e.message}"
                                }
                            }
                            showClearDialog = false
                        }
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}