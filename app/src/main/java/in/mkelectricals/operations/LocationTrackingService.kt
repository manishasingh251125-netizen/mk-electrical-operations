package in.mkelectricals.operations

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent?.action=="STOP"){stopTracking();stopSelf();return START_NOT_STICKY}
        val token=intent?.getStringExtra("token")?:return START_NOT_STICKY
        val uid=intent.getStringExtra("uid")?:return START_NOT_STICKY
        val attendance=intent.getStringExtra("attendance")?:return START_NOT_STICKY
        val site=intent.getStringExtra("site")?:return START_NOT_STICKY
        startForeground(41,notification())
        if(running.compareAndSet(false,true)){
            val req=LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,15_000L).setMinUpdateIntervalMillis(10_000L).build()
            callback=object:LocationCallback(){override fun onLocationResult(result:LocationResult){
                val l=result.lastLocation?:return
                scope.launch { try { ApiClient.pushLocation(token,uid,attendance,site,l.latitude,l.longitude,l.accuracy) } catch (_:Exception) { } }
            }}
            try{client.requestLocationUpdates(req,callback!!,mainLooper)}catch(_:SecurityException){stopSelf()}
        }
        return START_STICKY
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