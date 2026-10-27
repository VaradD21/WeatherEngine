package com.weatherengine.app.core.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.*
import com.weatherengine.app.WeatherEngineApp
import com.weatherengine.app.core.alert.HomeRainChecker
import com.weatherengine.app.core.notification.AlertNotificationManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class HomeRainAlertWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? WeatherEngineApp ?: return Result.success()
        val container = app.appContainer
        val settingsStore = container.settingsStore

        val enabled = settingsStore.homeRainAlertEnabledFlow.first()
        if (!enabled) {
            return Result.success()
        }

        val notificationsOn = settingsStore.notificationsEnabledFlow.first()
        if (!notificationsOn) {
            return Result.success()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                return Result.success()
            }
        }

        val homeLat = settingsStore.homeLatFlow.first()
        val homeLon = settingsStore.homeLonFlow.first()
        val homeName = settingsStore.homeNameFlow.first().ifBlank { "Home" }

        if (homeLat == null || homeLon == null) {
            return Result.success()
        }

        val rainResult = HomeRainChecker.checkRainAt(
            apiService = container.openMeteoClient.apiService,
            lat = homeLat,
            lon = homeLon
        ) ?: return Result.retry()

        if (rainResult.isRaining) {
            AlertNotificationManager.postHomeRainAlert(
                context = applicationContext,
                homeName = homeName,
                rainDetails = rainResult.description
            )
        }

        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "weatherengine_home_rain_check"

        fun enqueuePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWork = PeriodicWorkRequestBuilder<HomeRainAlertWorker>(
                1, TimeUnit.HOURS,
                15, TimeUnit.MINUTES // 15 min flex interval
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
            )
        }

        fun triggerImmediateCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTime = OneTimeWorkRequestBuilder<HomeRainAlertWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(oneTime)
        }
    }
}
