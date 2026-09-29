package com.weatherengine.app.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val label: String
)

interface LocationProvider {
    suspend fun getCurrentLocation(): DeviceLocation?
    suspend fun getPlaceName(lat: Double, lon: Double): String = ""
}

class AndroidLocationProvider(private val context: Context) : LocationProvider {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): DeviceLocation? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        if (!isGpsEnabled) {
            return null
        }

        return try {
            withTimeoutOrNull(8000) {
                val cts = CancellationTokenSource()
                val location: Location? = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cts.token
                ).await()

                val resolved = location ?: fusedLocationClient.lastLocation.await()
                resolved?.let { loc ->
                    DeviceLocation(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        label = getFriendlyLocationLabel(loc.latitude, loc.longitude)
                    )
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getPlaceName(lat: Double, lon: Double): String {
        return getFriendlyLocationLabel(lat, lon)
    }

    private fun getFriendlyLocationLabel(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val address = addresses?.firstOrNull()
            if (address != null) {
                val subLocality = address.subLocality
                val locality = address.locality ?: address.subAdminArea
                val adminArea = address.adminArea

                when {
                    !subLocality.isNullOrBlank() && !locality.isNullOrBlank() -> "$subLocality, $locality"
                    !locality.isNullOrBlank() && !adminArea.isNullOrBlank() -> "$locality, $adminArea"
                    !locality.isNullOrBlank() -> locality
                    !subLocality.isNullOrBlank() -> subLocality
                    !adminArea.isNullOrBlank() -> adminArea
                    !address.featureName.isNullOrBlank() -> address.featureName
                    else -> knownCityOrFallback(lat, lon)
                }
            } else {
                knownCityOrFallback(lat, lon)
            }
        } catch (_: Exception) {
            knownCityOrFallback(lat, lon)
        }
    }

    private fun knownCityOrFallback(lat: Double, lon: Double): String {
        if (abs(lat - 19.0760) < 0.1 && abs(lon - 72.8777) < 0.1) {
            return "Mumbai, Maharashtra"
        }
        return formatCoordinateFallback(lat, lon)
    }

    private fun formatCoordinateFallback(lat: Double, lon: Double): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lonDir = if (lon >= 0) "E" else "W"
        return String.format(Locale.US, "%.2f°%s, %.2f°%s", abs(lat), latDir, abs(lon), lonDir)
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }
        addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }
        addOnCanceledListener {
            continuation.cancel()
        }
    }
}
