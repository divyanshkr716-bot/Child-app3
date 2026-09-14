package com.example.engine

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ChildLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val speed: Float,
    val timestamp: Long,
    val provider: String
)

class RemoteLocationManager(private val context: Context) {

    companion object {
        private const val TAG = "RemoteLocationManager"
        private const val MIN_TIME_MS = 10_000L // 10s
        private const val MIN_DISTANCE_M = 5f    // 5m
    }

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _currentLocation = MutableStateFlow<ChildLocation?>(null)
    val currentLocation: StateFlow<ChildLocation?> = _currentLocation.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            updateLocation(location)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    @SuppressLint("MissingPermission")
    fun startTracking(): Boolean {
        if (_isTracking.value) return true

        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            Log.w(TAG, "Location permission not granted")
            return false
        }

        try {
            // First retrieve cached last known location
            var lastLocation: Location? = null
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
            if (lastLocation == null && locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
            lastLocation?.let { updateLocation(it) }

            // Register updates
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_MS,
                    MIN_DISTANCE_M,
                    locationListener,
                    Looper.getMainLooper()
                )
            }
            if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_MS,
                    MIN_DISTANCE_M,
                    locationListener,
                    Looper.getMainLooper()
                )
            }

            _isTracking.value = true
            Log.i(TAG, "Location tracking initiated")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location tracking", e)
            return false
        }
    }

    fun stopTracking() {
        try {
            locationManager?.removeUpdates(locationListener)
            _isTracking.value = false
            Log.i(TAG, "Location tracking stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location updates", e)
        }
    }

    private fun updateLocation(loc: Location) {
        _currentLocation.value = ChildLocation(
            latitude = loc.latitude,
            longitude = loc.longitude,
            accuracy = loc.accuracy,
            speed = loc.speed,
            timestamp = loc.time,
            provider = loc.provider ?: "GPS"
        )
        Log.d(TAG, "Updated child coordinates: (${loc.latitude}, ${loc.longitude})")
    }
}
