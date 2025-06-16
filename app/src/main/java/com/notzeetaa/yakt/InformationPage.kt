package com.notzeetaa.yakt

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            androidVersion = Build.VERSION.RELEASE ?: "Unknown"
            kernelVersion = getKernelVersion()
            gpuInformation = getGpuInformation()
            ramInformation = getTotalRam()

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
            CircularProgressIndicator()
        }

        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(500))
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoCard(stringResource(R.string.android_version), androidVersion)
                InfoCard(stringResource(R.string.kernel_version), kernelVersion)
                InfoCard(stringResource(R.string.gpu), gpuInformation)
                InfoCard(stringResource(R.string.total_ram), ramInformation)
                InfoCard(stringResource(R.string.battery_temperature), batteryTemp)
                InfoCard(stringResource(R.string.battery_capacity), batteryCapacity)
                InfoCard(stringResource(R.string.battery_health), batteryHealth)
            }
        }
    }
}

@Composable
fun InfoCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
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
