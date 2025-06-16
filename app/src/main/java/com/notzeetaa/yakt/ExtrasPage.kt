package com.notzeetaa.yakt

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Build
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.window.DialogProperties

@Composable
fun ExtrasPage(viewModel: MyViewModel, context: Context) {
    var showDnsDialog by remember { mutableStateOf(false) }
    val dnsOptions = listOf("google", "adguard", "automatic", "cloudflare")
    var showRootDialog by remember { mutableStateOf(false) }
    val onePlusBootCheck by viewModel.onePlusBootCheck.collectAsState() // Assuming OnePlusBootCheck is the correct flow from your ViewModel
    val framerateCheck by viewModel.framerateCheck.collectAsState()
    val isRooted = isDeviceRooted()
    var showOnePlusDialog by remember { mutableStateOf(false) }
    val isOnePlus = Build.MANUFACTURER.equals("OnePlus", ignoreCase = true)
    val isAndroid15OrAbove = Build.VERSION.SDK_INT >= 35
    var showDexDialog by remember { mutableStateOf(false) }
    val showDexApplyButton = viewModel.showDexApplyButton
    var selectedDexOption = viewModel.selectedDexOption
    var showProgressDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()


    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDnsDialog = true },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.dns_icon),
                    contentDescription = stringResource(R.string.dns_icon_description),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.select_dns_mode_title), fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.select_dns_mode_description),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp)) // Add some spacing between cards

        // Card for OnePlus Check
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = isRooted && isOnePlus) {
                    if (!isOnePlus) {
                        showOnePlusDialog = true
                    } else if (!isRooted) {
                        showRootDialog = true
                    } else {
                        val newValue = !onePlusBootCheck
                        viewModel.saveOnePlusBoot(newValue)
                        executeScript(context, "oneplus", newValue)
                    }
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isRooted && isOnePlus) MaterialTheme.colorScheme.surface else Color.Gray
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.fps_icon),
                    contentDescription = "Apply Boot Icon",
                    tint = if (isRooted && isOnePlus) MaterialTheme.colorScheme.primary else Color.DarkGray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.oneplus), fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.oneplus_description),
                        fontSize = 13.sp,
                        color = if (isRooted && isOnePlus)
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        else
                            Color.DarkGray
                    )
                }
                Switch(
                    checked = onePlusBootCheck,
                    onCheckedChange = null, // No manual toggle here; card handles it
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp)) // Add some spacing between cards

        // Card for disable game framerate Check
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = isRooted && isAndroid15OrAbove) {
                    if (!isAndroid15OrAbove) {
                        Toast.makeText(context, "This feature requires Android 15 or higher.", Toast.LENGTH_SHORT).show()
                    } else if (!isRooted) {
                        showRootDialog = true
                    } else {
                        val newValue = !framerateCheck
                        viewModel.saveframerateBoot(newValue)
                        executeScript(context, "framerate", newValue)
                    }
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isRooted && isAndroid15OrAbove) MaterialTheme.colorScheme.surface else Color.Gray
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.fps_icon),
                    contentDescription = "Framerate Icon",
                    tint = if (isRooted && isAndroid15OrAbove) MaterialTheme.colorScheme.primary else Color.DarkGray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.framerate), fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.framerate_description),
                        fontSize = 13.sp,
                        color = if (isRooted && isAndroid15OrAbove)
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        else
                            Color.DarkGray
                    )
                }
                Switch(
                    checked = framerateCheck, // You may want a separate variable for framerate state
                    onCheckedChange = null,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card for dex optimiazation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showDexDialog = true
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.dex_icon),
                        contentDescription = "Dex optimization Icon",
                        tint = if (isRooted) MaterialTheme.colorScheme.primary else Color.DarkGray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.dex), fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.dex_description),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }

                    if (showDexApplyButton && selectedDexOption != null) {
                        Button(onClick = {
                            coroutineScope.launch {
                                Toast.makeText(context, "Applying dex optimization $selectedDexOption, check logs to check the progress, should take some minutes", Toast.LENGTH_SHORT).show()
                                viewModel.clearDexSelection()
                                delay(300) // Optional: to allow Compose to show dialog
                                executeDexScript(context, "DEX", selectedDexOption!!)
                            }
                        }) {
                            Text("Apply $selectedDexOption")
                        }
                    }
                }
            }
        }
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

    if (showDexDialog) {
        AlertDialog(
            onDismissRequest = { showDexDialog = false },
            title = { Text("Select Dex optimization") },
            text = {
                Column {
                    listOf("Extreme", "Speed").forEach { mode ->
                        Button(
                            onClick = {
                                viewModel.selectDexOption(mode)
                                showDexDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(mode)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDexDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDnsDialog) {
        AlertDialog(
            onDismissRequest = { showDnsDialog = false },
            title = { Text("Select DNS Provider") },
            text = {
                Column {
                    dnsOptions.forEach { dnsOption ->
                        Button(
                            onClick = {
                                executeDnsScript(context, dnsOption)
                                showDnsDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = dnsOption.replaceFirstChar { it.uppercase() })
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDnsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRootDialog) {
        AlertDialog(
            onDismissRequest = { showRootDialog = false },
            title = { Text("Root Access Required") },
            text = { Text("This feature requires root access to function properly.") },
            confirmButton = {
                TextButton(onClick = { showRootDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showOnePlusDialog) {
        AlertDialog(
            onDismissRequest = { showOnePlusDialog = false },
            title = { Text("OnePlus Only Feature") },
            text = { Text("This feature is designed specifically for OnePlus devices and is not supported on your device.") },
            confirmButton = {
                TextButton(onClick = { showOnePlusDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

fun executeDnsScript(context: Context, dnsMode: String) {
    val scriptFile = copyAssetFileToStorage(context, "extras.sh")
    grantExecutePermission(scriptFile)

    val command = "su -c 'sh ${scriptFile.absolutePath} DNS $dnsMode'"

    try {
        // Start the process
        val process = ProcessBuilder("sh", "-c", command).start()

        // Capture standard output and error output
        val outputStream = process.inputStream.bufferedReader().use { it.readText() }
        val errorStream = process.errorStream.bufferedReader().use { it.readText() }

        // Wait for the process to complete
        val exitCode = process.waitFor()

        // Log outputs for debugging
        Log.d("DNS Script", "Command Output: $outputStream")
        Log.d("DNS Script", "Error Output: $errorStream")
        Log.d("DNS Script", "Exit Code: $exitCode")

        // Check if the script was successful
        if (exitCode == 0) {
            Toast.makeText(context, "$dnsMode DNS applied successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to apply $dnsMode DNS", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        // Log the exception error
        Log.e("DNS Script", "Error executing script: ${e.message}")
        Toast.makeText(context, "Error executing script", Toast.LENGTH_SHORT).show()
    }
}

fun executeScript(context: Context, Mode: String, isChecked: Boolean) {
    val scriptFile = copyAssetFileToStorage(context, "extras.sh")
    grantExecutePermission(scriptFile)

    val command = "su -c 'sh ${scriptFile.absolutePath} $Mode $isChecked'"

    try {
        // Start the process
        val process = ProcessBuilder("sh", "-c", command).start()

        // Capture standard output and error output
        val outputStream = process.inputStream.bufferedReader().use { it.readText() }
        val errorStream = process.errorStream.bufferedReader().use { it.readText() }

        // Wait for the process to complete
        val exitCode = process.waitFor()

        // Log outputs for debugging
        Log.d("OnePlus Script $Mode $isChecked", "Command Output: $outputStream")
        Log.d("OnePlus Script $Mode $isChecked", "Error Output: $errorStream")
        Log.d("OnePlus Script $Mode $isChecked", "Exit Code: $exitCode")

        // Check if the script was successful
        if (exitCode == 0) {
            Toast.makeText(context, "$Mode OnePlus applied successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to apply $Mode OnePlus, value of it is $isChecked", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        // Log the exception error
        Log.e("OnePlus Script", "Error executing script: ${e.message}")
        Toast.makeText(context, "Error executing script", Toast.LENGTH_SHORT).show()
    }
}

suspend fun executeDexScript(context: Context, mode: String, type: String) {
    withContext(Dispatchers.IO) {
        val scriptFile = copyAssetFileToStorage(context, "extras.sh")
        grantExecutePermission(scriptFile)

        val command = "su -c 'sh ${scriptFile.absolutePath} $mode $type'"

        try {
            val process = ProcessBuilder("sh", "-c", command).start()

            val outputStream = process.inputStream.bufferedReader().use { it.readText() }
            val errorStream = process.errorStream.bufferedReader().use { it.readText() }

            val exitCode = process.waitFor()

            Log.d("Dex Script $mode $type", "Command Output: $outputStream")
            Log.d("Dex Script $mode $type", "Error Output: $errorStream")
            Log.d("Dex Script $mode $type", "Exit Code: $exitCode")

            withContext(Dispatchers.Main) {
                if (exitCode == 0) {
                    Toast.makeText(context, "$mode applied successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to apply $mode, value: $type", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("Dex Script", "Error: ${e.message}")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error executing script", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

