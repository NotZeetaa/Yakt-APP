package com.notzeetaa.yakt

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

class BootWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val dataStoreManager = DataStoreManager(applicationContext)

            // Verificar se o apply at boot está ativado
            val applyAtBoot = dataStoreManager.applyAtBootFlow.first()
            if (!applyAtBoot) return@withContext Result.success()

            // Obter o modo selecionado
            val selectedMode = dataStoreManager.selectedModeFlow.first()

            // Determinar qual script usar
            val scriptName = when (selectedMode) {
                0 -> "battery"
                1 -> "balanced"
                2 -> "gaming"
                3 -> "latency"
                else -> return@withContext Result.failure()
            }

            val currentMode = when (selectedMode) {
                0 -> "battery"
                1 -> "balanced"
                2 -> "gaming"
                3 -> "latency"
                else -> "balanced"
            }

            // Copiar e executar o script
            val scriptFile = copyAssetFileToStorage(applicationContext, scriptName)
            grantExecutePermission(scriptFile)
            executeShellScript(scriptFile, currentMode)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}