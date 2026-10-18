package com.weatherengine.app.core.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.*
import com.weatherengine.app.WeatherEngineApp
import com.weatherengine.app.core.notification.AlertNotificationManager
import com.weatherengine.app.data.api.NetworkResult
import com.weatherengine.app.data.location.AndroidLocationProvider
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class LocationTrackingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? WeatherEngineApp ?: return Result.success()
        val container = app.appContainer

        val token = container.settingsStore.tokenFlow.first()
        if (token.isNullOrBlank()) {
            return Result.success()
        }

        val fineGranted = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            return Result.success()
        }

        val locationProvider = AndroidLocationProvider(applicationContext)
        val location = locationProvider.getCurrentLocation() ?: return Result.retry()

        val result = container.remoteRepository.recordLocationHistory(
            lat = location.latitude,
            lon = location.longitude
        )

        if (result is NetworkResult.Success) {
            val response = result.data
            response.predictedAlert?.let { alert ->
                AlertNotificationManager.postPredictiveWeatherAlert(
                    context = applicationContext,
                    title = alert.headline,
                    message = alert.message,
                    alertKey = "${alert.alertType}_${alert.targetHour}"
                )
            }
        }

        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "weatherengine_hourly_location_tracking"

        fun enqueueHourly(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWork = PeriodicWorkRequestBuilder<LocationTrackingWorker>(
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
    }
}
