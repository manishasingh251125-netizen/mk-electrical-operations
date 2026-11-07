package in.mkelectricals.operations

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationTrackingService : Service() {
    private val client by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            getSharedPreferences("mk_shift", MODE_PRIVATE).edit()
                .putLong("last_lat", (location.latitude * 1_000_000).toLong())
                .putLong("last_lng", (location.longitude * 1_000_000).toLong())
                .putLong("last_location_at", location.time).apply()
            // Secure API sync is connected in the next integration step; no location is sent to a third party.
        }
    }
    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("mk_location", "Shift location", NotificationManager.IMPORTANCE_LOW))
        val notification: Notification = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, "mk_location")
            .setContentTitle("MK Electricals attendance").setContentText("Location tracking is active during your shift.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation).build()
        else Notification.Builder(this).setContentTitle("MK Electricals attendance").setContentText("Location tracking is active.").setSmallIcon(android.R.drawable.ic_menu_mylocation).build()
        startForeground(41, notification)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15_000L).setMinUpdateIntervalMillis(10_000L).build()
        try { client.requestLocationUpdates(request, callback, mainLooper) } catch (_: SecurityException) { stopSelf() }
    }
    override fun onDestroy() { client.removeLocationUpdates(callback); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
