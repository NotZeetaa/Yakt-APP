package com.notzeetaa.yakt

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import coil.compose.AsyncImage
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import java.io.BufferedReader
import java.io.InputStreamReader
import android.content.Intent
import androidx.compose.material.icons.filled.Settings
import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.File
// Add these imports at the top of your file
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay

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
    navController: NavHostController // Remove default value - it will be passed from MainActivity
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
    // Add this state variable to your HomePage
    var showAIModeAppsDialog by remember { mutableStateOf(false) }

    // Navigation state for log viewer
    var navigateToLogViewer by remember { mutableStateOf(false) }

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

            // Card 4 - Card log (now navigates to LogViewerPage) - FIXED
            ReusableCard(
                iconId = R.drawable.log_icon,
                title = stringResource(R.string.view_log_title),
                description = stringResource(R.string.view_log_description),
                isEnabled = isRooted,
                onClick = {
                    if (isRooted) {
                        // Simple direct navigation with try-catch
                        try {
                            navController.navigate(AppScreen.LOG_VIEWER.name)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Navigation failed", Toast.LENGTH_SHORT).show()
                        }
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

            // Add this to your HomePage where you want the card to appear
            Spacer(modifier = Modifier.height(16.dp))

// Card 5 - AI Mode Apps
            ReusableCard(
                iconId = R.drawable.ai_apps_icon, // You can use R.drawable.ai_icon or create a new one
                title = "AI Mode Apps",
                description = "Manage apps that will use AI optimization",
                isEnabled = isRooted,
                onClick = {
                    if (isRooted) {
                        showAIModeAppsDialog = true
                    } else {
                        showRootDialog = true
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



// Add this dialog to your HomePage (with the other dialogs)
        if (showAIModeAppsDialog) {
            AIModeAppsDialog(
                onDismiss = { showAIModeAppsDialog = false },
                viewModel = viewModel
            )
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

// Simplified Log Viewer Page
@OptIn(ExperimentalMaterial3Api::class)
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
            "No log file found"
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Script Logs",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showClearDialog = true },
                        enabled = logContent.isNotEmpty() && logContent != "No log file found"
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Log")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                if (logContent.isNotEmpty() && logContent != "No log file found") {
                    val logLines = logContent.split("\n")

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logLines) { line ->
                            if (line.isNotBlank()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when {
                                            line.contains("[USER_ACTION]") -> MaterialTheme.colorScheme.tertiaryContainer
                                            line.contains("[ERROR]") -> MaterialTheme.colorScheme.errorContainer
                                            line.contains("[SCRIPT]") -> MaterialTheme.colorScheme.secondaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceContainer
                                        }
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Text(
                                        text = line,
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = when {
                                            line.contains("[USER_ACTION]") -> MaterialTheme.colorScheme.onTertiaryContainer
                                            line.contains("[ERROR]") -> MaterialTheme.colorScheme.onErrorContainer
                                            line.contains("[SCRIPT]") -> MaterialTheme.colorScheme.onSecondaryContainer
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Log summary
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Text(
                            text = "${logLines.size} log entries",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = logContent,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// AI Mode Apps Dialog Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIModeAppsDialog(
    onDismiss: () -> Unit,
    viewModel: MyViewModel
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var userApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var selectedApps by remember { mutableStateOf<Set<String>>(emptySet()) }
    var searchQuery by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Load apps and selected list on dialog open
    LaunchedEffect(Unit) {
        isLoading = true
        val (apps, selected) = loadAppsAndSelectedList(context)
        userApps = apps
        selectedApps = selected
        isLoading = false
    }

    // Filter apps based on search
    val filteredApps = remember(userApps, selectedApps, searchQuery) {
        if (searchQuery.isEmpty()) {
            // Show AI-optimized apps first, then others
            val aiOptimizedApps = userApps.filter { it.packageName in selectedApps }
            val otherApps = userApps.filter { it.packageName !in selectedApps }
            aiOptimizedApps + otherApps
        } else {
            userApps.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }.sortedBy { if (it.packageName in selectedApps) 0 else 1 }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.8f)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "AI Mode Apps",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${selectedApps.size} apps optimized for AI",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Search Bar
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    "Search apps...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Apps List
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Loading apps...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "No apps",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No apps found" else "No user apps available",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        state = scrollState,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredApps) { app ->
                            AIModeAppItem(
                                app = app,
                                isSelected = app.packageName in selectedApps,
                                onToggle = { packageName, isChecked ->
                                    coroutineScope.launch {
                                        if (isChecked) {
                                            selectedApps = selectedApps + packageName
                                        } else {
                                            selectedApps = selectedApps - packageName
                                        }
                                        saveSelectedApps(context, selectedApps)
                                    }
                                }
                            )
                        }
                    }
                }

                // Footer
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredApps.size} apps • ${selectedApps.size} AI optimized",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}

// AI Mode App Item Composable
@Composable
fun AIModeAppItem(
    app: AppInfo,
    isSelected: Boolean,
    onToggle: (String, Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Custom app icon with proper fallback
            AppIconWithFallback(
                app = app,
                isSelected = isSelected,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Switch(
                checked = isSelected,
                onCheckedChange = { onToggle(app.packageName, it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
fun AppIconWithFallback(
    app: AppInfo,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    var hasError by remember { mutableStateOf(false) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (hasError) {
            // Show fallback letter icon
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.name.take(1).uppercase(),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        } else {
            // Try to load app icon
            AsyncImage(
                model = app.applicationInfo.toIconUri(),
                contentDescription = app.name,
                modifier = Modifier.matchParentSize(),
                onError = { hasError = true }
            )
        }
    }
}

// Extension function to convert ApplicationInfo to URI for Coil
fun android.content.pm.ApplicationInfo.toIconUri(): String {
    return "android.resource://${packageName}/${icon}"
}

// AppInfo data class
data class AppInfo(
    val name: String,
    val packageName: String,
    val applicationInfo: android.content.pm.ApplicationInfo
)

private suspend fun loadAppsAndSelectedList(context: Context): Pair<List<AppInfo>, Set<String>> {
    return withContext(Dispatchers.IO) {
        val pm = context.packageManager

        // Load selected apps from file using su command
        val selectedApps = try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat /data/local/game_list.conf"))
            val inputStream = process.inputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.readLines()
            process.waitFor()

            lines.map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }

        // Get installed packages with query intent
        val userApps = mutableListOf<AppInfo>()

        try {
            // Query for launcher apps
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfos = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)

            resolveInfos.forEach { resolveInfo ->
                val appInfo = resolveInfo.activityInfo.applicationInfo
                // Filter out system apps
                if (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0) {
                    val name = pm.getApplicationLabel(appInfo).toString()
                    userApps.add(AppInfo(name, appInfo.packageName, appInfo))
                }
            }

            // Include packages from selectedApps that might not have launcher intent
            selectedApps.forEach { packageName ->
                if (userApps.none { it.packageName == packageName }) {
                    try {
                        val appInfo = pm.getApplicationInfo(packageName, 0)
                        if (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0) {
                            val name = pm.getApplicationLabel(appInfo).toString()
                            userApps.add(AppInfo(name, packageName, appInfo))
                        }
                    } catch (e: Exception) {
                        // App might be uninstalled, skip it
                    }
                }
            }

        } catch (e: Exception) {
            // Handle error silently
        }

        // Remove duplicates and sort
        val uniqueApps = userApps.distinctBy { it.packageName }
            .sortedBy { it.name.lowercase() }

        Pair(uniqueApps, selectedApps)
    }
}

private suspend fun saveSelectedApps(context: Context, selectedApps: Set<String>) {
    withContext(Dispatchers.IO) {
        try {
            // Read existing package names from the file using su
            val allExistingPackageNames = try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat /data/local/game_list.conf"))
                val inputStream = process.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val lines = reader.readLines()
                process.waitFor()

                lines.map { it.trim() }
                    .filter { it.isNotBlank() }
                    .toSet()
            } catch (e: Exception) {
                emptySet()
            }

            // Get the list of user-installed apps
            val userAppsPackageNames = try {
                val pm = context.packageManager
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
                resolveInfos.map { it.activityInfo.applicationInfo.packageName }
                    .filter { packageName ->
                        try {
                            val appInfo = pm.getApplicationInfo(packageName, 0)
                            appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0
                        } catch (e: Exception) {
                            false
                        }
                    }.toSet()
            } catch (e: Exception) {
                emptySet()
            }

            // Preserve package names that are NOT user apps
            val preservedPackageNames = allExistingPackageNames - userAppsPackageNames

            // Combine preserved package names with newly selected user apps
            val finalPackageNames = preservedPackageNames + selectedApps

            val content = finalPackageNames.joinToString("\n")

            // Create directory and write file using su
            Runtime.getRuntime().exec(arrayOf("su", "-c", "mkdir -p /data/local/")).waitFor()
            val writeCommand = "printf '%s' \"$content\" > /data/local/game_list.conf"
            Runtime.getRuntime().exec(arrayOf("su", "-c", writeCommand)).waitFor()

        } catch (e: Exception) {
            // Handle error silently
        }
    }
}