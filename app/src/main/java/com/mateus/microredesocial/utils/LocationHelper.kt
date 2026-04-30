package com.mateus.microredesocial.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.navigationevent.NavigationEventDispatcher
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

object LocationHelper {

    @SuppressLint("MissingPermission")
    fun getCurrentCity(
        context: Context,
        onResult: (String?) -> Unit
    ) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.getCurrentLocation(NavigationEventDispatcher.Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    getCityFromLocation(context, location.latitude, location.longitude, onResult)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    private fun getCityFromLocation(
        context: Context,
        latitude: Double,
        longitude: Double,
        onResult: (String?) -> Unit
    ) {
        val geocoder = Geocoder(context, Locale.getDefault())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                val city = addresses.firstOrNull()?.let {
                    it.locality ?: it.subAdminArea ?: it.adminArea
                }
                onResult(city)
            }
        } else {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            val city = addresses?.firstOrNull()?.let {
                it.locality ?: it.subAdminArea ?: it.adminArea
            }
            onResult(city)
        }
    }
}
