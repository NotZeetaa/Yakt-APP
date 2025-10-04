package com.notzeetaa.yakt

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.ceil
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight

@Composable
fun InformationPage(viewModel: MyViewModel) {
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(true) }

    var androidVersion by remember { mutableStateOf("Loading...") }
    var kernelVersion by remember { mutableStateOf("Loading...") }
    var gpuInformation by remember { mutableStateOf("Loading...") }
    var ramInformation by remember { mutableStateOf("Loading...") }
    var batteryTemp by remember { mutableStateOf("Loading...") }
    var batteryCapacity by remember { mutableStateOf("Loading...") }
    var batteryHealth by remember { mutableStateOf("Loading...") }
    var appVersion by remember { mutableStateOf("Loading...") }

    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            androidVersion = Build.VERSION.RELEASE ?: "Unknown"
            kernelVersion = getKernelVersion()
            gpuInformation = getGpuInformation()
            ramInformation = getTotalRam()
            appVersion = getAppVersion(context)

            val (temp, health) = getBatteryStatusInfo(context)
            batteryTemp = temp
            batteryHealth = health
            batteryCapacity = getBatteryCapacitySmart(context)
        }

        kotlinx.coroutines.delay(200)
        isLoading = false
        showContent = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(500))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 3.dp
                )
                Text(
                    text = "Loading system information...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(500)) + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut(tween(300))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // System Information Section
                Text(
                    text = "System Information",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )

                ModernInfoCard(
                    title = stringResource(R.string.android_version),
                    value = androidVersion,
                    icon = Icons.Filled.Info,
                    iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer
                )

                ModernInfoCard(
                    title = stringResource(R.string.kernel_version),
                    value = kernelVersion,
                    icon = Icons.Filled.Settings,
                    iconBackgroundColor = MaterialTheme.colorScheme.secondaryContainer
                )

                ModernInfoCard(
                    title = stringResource(R.string.gpu),
                    value = gpuInformation,
                    icon = Icons.Filled.Info,
                    iconBackgroundColor = MaterialTheme.colorScheme.tertiaryContainer
                )

                ModernInfoCard(
                    title = stringResource(R.string.total_ram),
                    value = ramInformation,
                    icon = Icons.Filled.Info,
                    iconBackgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                )

                // Battery Information Section
                Text(
                    text = "Battery Information",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp))
                )

                ModernInfoCard(
                    title = stringResource(R.string.battery_temperature),
                    value = batteryTemp,
                    icon = Icons.Filled.Info,
                    iconBackgroundColor = MaterialTheme.colorScheme.errorContainer
                )

                ModernInfoCard(
                    title = stringResource(R.string.battery_capacity),
                    value = batteryCapacity,
                    icon = Icons.Filled.Info,
                    iconBackgroundColor = MaterialTheme.colorScheme.secondaryContainer
                )

                ModernInfoCard(
                    title = stringResource(R.string.battery_health),
                    value = batteryHealth,
                    icon = Icons.Filled.Info,
                    iconBackgroundColor = when (batteryHealth) {
                        "Good" -> MaterialTheme.colorScheme.primaryContainer
                        "Overheat" -> MaterialTheme.colorScheme.errorContainer
                        "Dead" -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )

                // App Information Section
                Text(
                    text = "App Information",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp))
                )

                ModernInfoCard(
                    title = "App Version",
                    value = appVersion,
                    icon = Icons.Filled.Info,
                    iconBackgroundColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            }
        }
    }
}

@Composable
fun ModernInfoCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconBackgroundColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon with background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Text content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// System Information Retrieval Methods
// ─────────────────────────────────────────────

fun getKernelVersion(): String = try {
    val process = Runtime.getRuntime().exec("uname -r")
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    reader.readLine() ?: "Unknown"
} catch (e: Exception) {
    "Unknown"
}

fun getGpuInformation(): String = try {
    val process = Runtime.getRuntime().exec("cat /sys/class/kgsl/kgsl-3d0/gpu_model")
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    reader.readLine() ?: "Unknown"
} catch (e: Exception) {
    "Unknown"
}

fun getTotalRam(): String {
    return try {
        val process = Runtime.getRuntime().exec("grep MemTotal /proc/meminfo")
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val memInfo = reader.readLine() ?: return "Unknown"
        val totalRamKb = memInfo.split(Regex("\\s+")).getOrNull(1)?.toLongOrNull() ?: return "Unknown"
        val totalRamGb = ceil(totalRamKb / 1024.0 / 1024.0).toInt()

        // Round up to nearest standard size
        val standardSizes = listOf(2, 4, 6, 8, 12, 16, 24, 32, 48, 64, 96, 128)
        val roundedSize = standardSizes.firstOrNull { it >= totalRamGb } ?: totalRamGb

        "$roundedSize GB"
    } catch (e: Exception) {
        "Unknown"
    }
}

fun getBatteryStatusInfo(context: Context): Pair<String, String> {
    val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    val batteryStatus = context.registerReceiver(null, intentFilter)

    val temperature = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)?.let {
        it / 10.0
    } ?: -1.0
    val tempStr = if (temperature >= 0) String.format("%.1f ºC", temperature) else "Unknown"

    val health = when (batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
        else -> "Unknown"
    }

    return Pair(tempStr, health)
}

fun getBatteryCapacitySmart(context: Context): String {
    val sysfs = getBatteryCapacityFromSysfs()
    if (sysfs != "Unknown") return sysfs

    val reflection = getBatteryCapacityFromProfile(context)
    return reflection
}

fun getBatteryCapacityFromSysfs(): String {
    return try {
        val process = Runtime.getRuntime().exec("cat /sys/class/power_supply/battery/charge_full_design")
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val line = reader.readLine()
        val capacity = line?.toIntOrNull() ?: return "Unknown"
        "${capacity / 1000} mAh" // Convert µAh to mAh
    } catch (e: Exception) {
        "Unknown"
    }
}

fun getBatteryCapacityFromProfile(context: Context): String = try {
    val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
    val constructor = powerProfileClass.getConstructor(Context::class.java)
    val powerProfile = constructor.newInstance(context)
    val batteryCapacity = powerProfileClass
        .getMethod("getBatteryCapacity")
        .invoke(powerProfile) as Double
    "${batteryCapacity.toInt()} mAh"
} catch (e: Exception) {
    "Unknown"
}

// New function to get app version
fun getAppVersion(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        "${packageInfo.versionName} (${packageInfo.versionCode})"
    } catch (e: Exception) {
        "Unknown"
    }
}