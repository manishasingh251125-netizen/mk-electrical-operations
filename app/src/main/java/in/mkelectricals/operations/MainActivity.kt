package in.mkelectricals.operations

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Wine=Color(0xFF641B32); private val Deep=Color(0xFF35101F); private val Gold=Color(0xFFC49A58)

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{MaterialTheme(colorScheme=darkColorScheme(primary=Wine,secondary=Gold,background=Deep)){OperationsApp()}}}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OperationsApp(){
 val context=androidx.compose.ui.platform.LocalContext.current
 val scope= rememberCoroutineScope()
 var username by remember{mutableStateOf("")};var password by remember{mutableStateOf("")}
 var session by remember{mutableStateOf<ApiClient.Session?>(null)}
 var sites by remember{mutableStateOf(emptyList<ApiClient.Site>())};var selected by remember{mutableStateOf<ApiClient.Site?>(null)}
 var attendanceId by remember{mutableStateOf<String?>(null)};var busy by remember{mutableStateOf(false)}
 var message by remember{mutableStateOf("Sign in with your company username and password")}
 var tracking by remember{mutableStateOf(false)}
 val locationClient= remember{LocationServices.getFusedLocationProviderClient(context)}
 val pulse= rememberInfiniteTransition(label="pulse");val rotation by pulse.animateFloat(0f,360f,infiniteRepeatable(tween(16000,easing=LinearEasing)),label="spin")
 fun loadSites(s:ApiClient.Session){scope.launch{try{val loaded=withContext(Dispatchers.IO){ApiClient.assignedSites(s.accessToken,s.userId)};sites=loaded;selected=loaded.firstOrNull();if(loaded.isEmpty())message="No active site assigned yet. Ask the owner to assign your site."}catch(e:Exception){message=e.message?: "Could not load assigned sites."}}}
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){g->
   if(g[Manifest.permission.ACCESS_FINE_LOCATION]==true){message="Location permission granted. Tap Clock in again."}else message="Precise location permission is required for geofenced attendance."
 }
 Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Deep,Wine,Color(0xFF13090E))))){
  Column(Modifier.fillMaxSize().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
   Box(Modifier.size(116.dp).graphicsLayer{rotationZ=rotation;cameraDistance=18*density}.shadow(25.dp,RoundedCornerShape(34.dp)).background(Brush.linearGradient(listOf(Gold,Wine,Color(0xFF9C5268))),RoundedCornerShape(34.dp)),contentAlignment=Alignment.Center){Text("MK",color=Color.White,style=MaterialTheme.typography.displayLarge,fontWeight=FontWeight.Black)}
   Spacer(Modifier.height(16.dp));Text("MK ELECTRICALS",color=Gold,fontWeight=FontWeight.Bold);Text(if(session==null)"Field Operations" else "Hello, ${session?.name}",color=Color.White,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
   Spacer(Modifier.height(16.dp))
   Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(26.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFFDF9F6))){
    Column(Modifier.padding(20.dp)){
     if(session==null){
      Text("Employee sign in",color=Deep,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
      OutlinedTextField(username,{username=it},label={Text("Username")},singleLine=true,modifier=Modifier.fillMaxWidth())
      OutlinedTextField(password,{password=it},label={Text("Password")},singleLine=true,modifier=Modifier.fillMaxWidth())
      Button(enabled=!busy&&username.isNotBlank()&&password.isNotBlank(),onClick={busy=true;message="Signing in…";scope.launch{try{val s=withContext(Dispatchers.IO){ApiClient.login(username,password)};session=s;message="Signed in securely.";loadSites(s)}catch(e:Exception){message=e.message?: "Sign in failed."}finally{busy=false}}},modifier=Modifier.fillMaxWidth().padding(top=12.dp)){Text(if(busy)"Please wait…" else "Sign in")}
     }else{
      Text("Today's attendance",color=Deep,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
      Text(if(tracking)"● LIVE LOCATION ACTIVE" else "○ Not clocked in",color=if(tracking)Color(0xFF16834A)else Wine,fontWeight=FontWeight.Bold)
      if(sites.isNotEmpty()) { var expanded by remember{mutableStateOf(false)}; ExposedDropdownMenuBox(expanded=expanded,onExpandedChange={expanded=it}){OutlinedTextField(selected?.name?: "Choose site",{},readOnly=true,label={Text("Assigned site")},modifier=Modifier.menuAnchor().fillMaxWidth());ExposedDropdownMenu(expanded=expanded,onDismissRequest={expanded=false}){sites.forEach{site->DropdownMenuItem(text={Text(site.name)},onClick={selected=site;expanded=false})}}} }
      Button(enabled=!busy&&selected!=null,onClick={
       if(!tracking){
        val fine=ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
        if(!fine){val p=mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION);if(Build.VERSION.SDK_INT>=33)p.add(Manifest.permission.POST_NOTIFICATIONS);permission.launch(p)}
        else {busy=true;scope.launch{try{
         val loc=withContext(Dispatchers.IO){locationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,null).get()}
         if(loc==null)throw Exception("Unable to get GPS location. Turn on Location and try again.")
         val site=selected!!;val distance=FloatArray(1);android.location.Location.distanceBetween(loc.latitude,loc.longitude,site.lat,site.lng,distance)
         if(distance[0]>site.radius)throw Exception("You are ${distance[0].toInt()} m away. Clock-in allowed only within ${site.radius} m of ${site.name}.")
         val s=session!!;val id=withContext(Dispatchers.IO){ApiClient.clockIn(s.accessToken,s.userId,site,loc.latitude,loc.longitude)}
         attendanceId=id;val intent=Intent(context,LocationTrackingService::class.java).putExtra("token",s.accessToken).putExtra("refreshToken",s.refreshToken).putExtra("uid",s.userId).putExtra("attendance",id).putExtra("site",site.id).putExtra("siteLat",site.lat).putExtra("siteLng",site.lng).putExtra("radius",site.radius)
         ContextCompat.startForegroundService(context,intent);tracking=true;message="Clocked in at ${site.name}. Live GPS tracking is on."
        }catch(e:Exception){message=e.message?: "Clock-in failed."}finally{busy=false}}}
       }else{busy=true;scope.launch{try{val s=session!!;val id=attendanceId!!;val loc=withContext(Dispatchers.IO){locationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,null).get()};if(loc==null)throw Exception("GPS unavailable. Try again.");withContext(Dispatchers.IO){ApiClient.clockOut(s.accessToken,id,loc.latitude,loc.longitude)};context.startService(Intent(context,LocationTrackingService::class.java).setAction("STOP"));tracking=false;attendanceId=null;message="Clocked out. Location tracking stopped."}catch(e:Exception){message=e.message?: "Clock-out failed."}finally{busy=false}}}
      },modifier=Modifier.fillMaxWidth().padding(top=14.dp)){Text(if(busy)"Please wait…" else if(tracking)"Clock out" else "Clock in")}
      OutlinedButton(onClick={context.startService(Intent(context,LocationTrackingService::class.java).setAction("STOP"));session=null;tracking=false;attendanceId=null},modifier=Modifier.fillMaxWidth()){Text("Sign out")}
     }
     Spacer(Modifier.height(10.dp));Text(message,color=Color(0xFF6D5961),style=MaterialTheme.typography.bodySmall)
    }
   }
   Spacer(Modifier.height(14.dp));Text("Secure • Site-aware • Built for field teams",color=Color(0xFFE9D8DE),style=MaterialTheme.typography.bodySmall)
  }
 }
}
