package in.mkelectricals.operations

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.location.Location
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class LocationTrackingService : Service() {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val client by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var callback:LocationCallback?=null
    private val running=AtomicBoolean(false)
    private var outsideGeofence=false
    private var siteLat=0.0
    private var siteLng=0.0
    private var radius=150
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent?.action=="STOP"){stopTracking();stopSelf();return START_NOT_STICKY}
        var token=intent?.getStringExtra("token")?:return START_NOT_STICKY
        var refreshToken=intent.getStringExtra("refreshToken")?:return START_NOT_STICKY
        val uid=intent.getStringExtra("uid")?:return START_NOT_STICKY
        val attendance=intent.getStringExtra("attendance")?:return START_NOT_STICKY
        val site=intent.getStringExtra("site")?:return START_NOT_STICKY
        siteLat=intent.getDoubleExtra("siteLat",0.0);siteLng=intent.getDoubleExtra("siteLng",0.0);radius=intent.getIntExtra("radius",150);outsideGeofence=false
        startForeground(41,notification())
        if(running.compareAndSet(false,true)){
            val req=LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,15_000L).setMinUpdateIntervalMillis(10_000L).build()
            callback=object:LocationCallback(){override fun onLocationResult(result:LocationResult){
                val l=result.lastLocation?:return
                val distance=FloatArray(1);Location.distanceBetween(l.latitude,l.longitude,siteLat,siteLng,distance)
                val isOutside=distance[0]>radius
                if(isOutside&&!outsideGeofence)showGeofenceAlert(distance[0].toInt())
                outsideGeofence=isOutside
                scope.launch { try { ApiClient.pushLocation(token,uid,attendance,site,l.latitude,l.longitude,l.accuracy) } catch (_:Exception) { try { val renewed=ApiClient.refreshAccessToken(refreshToken); token=renewed.accessToken; refreshToken=renewed.refreshToken; ApiClient.pushLocation(token,uid,attendance,site,l.latitude,l.longitude,l.accuracy) } catch (_:Exception) { } } }
            }}
            try{client.requestLocationUpdates(req,callback!!,mainLooper)}catch(_:SecurityException){stopSelf()}
        }
        return START_STICKY
    }
    private fun showGeofenceAlert(distance:Int){
        val manager=getSystemService(NotificationManager::class.java)
        if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(NotificationChannel("mk_geofence","Site boundary alerts",NotificationManager.IMPORTANCE_HIGH))
        val n=if(Build.VERSION.SDK_INT>=26)Notification.Builder(this,"mk_geofence").setContentTitle("MK Electricals: Site boundary crossed").setContentText("You are about $distance m from the assigned site. Return to the geofence or contact your supervisor.").setSmallIcon(android.R.drawable.ic_dialog_alert).setAutoCancel(true).build() else Notification.Builder(this).setContentTitle("Site boundary crossed").setContentText("You are about $distance m from the assigned site.").setSmallIcon(android.R.drawable.ic_dialog_alert).build()
        manager.notify(42,n)
    }
    private fun notification():Notification{
        val manager=getSystemService(NotificationManager::class.java)
        if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(NotificationChannel("mk_location","Shift location",NotificationManager.IMPORTANCE_LOW))
        return if(Build.VERSION.SDK_INT>=26) Notification.Builder(this,"mk_location").setContentTitle("MK Electricals attendance").setContentText("Live location is active until clock-out.").setSmallIcon(android.R.drawable.ic_menu_mylocation).build()
        else Notification.Builder(this).setContentTitle("MK Electricals attendance").setContentText("Live location is active.").setSmallIcon(android.R.drawable.ic_menu_mylocation).build()
    }
    private fun stopTracking(){callback?.let{client.removeLocationUpdates(it)};callback=null;running.set(false)}
    override fun onDestroy(){stopTracking();scope.cancel();super.onDestroy()}
    override fun onBind(intent:Intent?):IBinder?=null
}