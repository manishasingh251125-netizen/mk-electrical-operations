package `in`.mkelectricals.operations

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

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
 var loginRole by remember{mutableStateOf("Employee")};var passwordVisible by remember{mutableStateOf(false)}
 var session by remember{mutableStateOf<ApiClient.Session?>(null)}
 var sites by remember{mutableStateOf(emptyList<ApiClient.Site>())};var selected by remember{mutableStateOf<ApiClient.Site?>(null)}
 var attendanceId by remember{mutableStateOf<String?>(null)};var busy by remember{mutableStateOf(false)}
 var message by remember{mutableStateOf("Sign in with your company username and password")}
 var tracking by remember{mutableStateOf(false)};var showBackgroundPrompt by remember{mutableStateOf(false)}
 val locationClient= remember{LocationServices.getFusedLocationProviderClient(context)}
 val authPrefs=remember{context.getSharedPreferences("mk_auth_session",android.content.Context.MODE_PRIVATE)}
 fun loadSites(s:ApiClient.Session){scope.launch{try{val loaded=withContext(Dispatchers.IO){ApiClient.assignedSites(s.accessToken,s.userId)};sites=loaded;selected=loaded.firstOrNull();if(loaded.isEmpty())message="No active site assigned yet. Ask the owner to assign your site."}catch(e:Exception){message=e.message?: "Could not load assigned sites."}}}
  LaunchedEffect(Unit){
   val saved=authPrefs.getString("refresh_token",null)
   if(saved!=null)try{
    val restored=withContext(Dispatchers.IO){ApiClient.refreshAccessToken(saved)}
    session=restored;authPrefs.edit().putString("refresh_token",restored.refreshToken).apply();message="Session restored."
    if(restored.role!="owner"){
     val assigned=withContext(Dispatchers.IO){ApiClient.assignedSites(restored.accessToken,restored.userId)}
     sites=assigned;selected=assigned.firstOrNull()
     val active=withContext(Dispatchers.IO){ApiClient.activeAttendance(restored.accessToken,restored.userId)}
     if(active!=null){
      val activeSite=assigned.firstOrNull{it.id==active.optString("site_id")}
      if(activeSite!=null){
       attendanceId=active.optString("id");tracking=true;selected=activeSite
       val service=Intent(context,LocationTrackingService::class.java).putExtra("token",restored.accessToken).putExtra("refreshToken",restored.refreshToken).putExtra("uid",restored.userId).putExtra("attendance",attendanceId).putExtra("site",activeSite.id).putExtra("siteLat",activeSite.lat).putExtra("siteLng",activeSite.lng).putExtra("radius",activeSite.radius)
       ContextCompat.startForegroundService(context,service);message="Active shift restored. Live GPS tracking is on."
      }else message="An active attendance record exists, but its site is no longer assigned. Contact the owner."
     }else if(assigned.isEmpty())message="No active site assigned yet. Ask the owner to assign your site."
    }
   }catch(e:Exception){authPrefs.edit().clear().apply();message=e.message?:"Please sign in again."}
  }
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){g->
   if(g[Manifest.permission.ACCESS_FINE_LOCATION]==true){
    message=if(Build.VERSION.SDK_INT>=33&&g[Manifest.permission.POST_NOTIFICATIONS]==false)"Location granted. Enable notifications for shift and site-boundary alerts, then tap Clock in again." else "Permissions updated. Tap Clock in again."
   }else message="Precise location permission is required for geofenced attendance."
 }
 Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Deep,Wine,Color(0xFF13090E))))){
  if(showBackgroundPrompt){AlertDialog(onDismissRequest={showBackgroundPrompt=false},title={Text("Allow background location")},text={Text("To keep attendance GPS active when the screen is locked, set MK Electricals location access to Allow all the time in App permissions.")},confirmButton={TextButton(onClick={showBackgroundPrompt=false;context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+context.packageName)))}){Text("Open app settings")}},dismissButton={TextButton(onClick={showBackgroundPrompt=false}){Text("Not now")}})}
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=26.dp, vertical=30.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
   Spacer(Modifier.height(24.dp))
   Text("M⚡K",color=Color(0xFFF8F4F4),style=MaterialTheme.typography.displayLarge,fontWeight=FontWeight.Black)
   Text("ELECTRICALS",color=Color(0xFFE7DDE0),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Medium,letterSpacing=4.sp)
   Spacer(Modifier.height(76.dp))
   if(session==null){
    Text("Sign In",modifier=Modifier.fillMaxWidth(),color=Color.White,style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
    Spacer(Modifier.height(26.dp))
    Row(Modifier.fillMaxWidth().background(Color(0xFF280711),RoundedCornerShape(50.dp)).padding(4.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){
     listOf("Employee","Owner").forEach { role ->
      val active=loginRole==role
      Box(Modifier.weight(1f).background(if(active)Color(0xFFB00035) else Color.Transparent,RoundedCornerShape(50.dp)).padding(vertical=15.dp),contentAlignment=Alignment.Center){
       Text(role,color=Color.White,fontWeight=if(active)FontWeight.Bold else FontWeight.Medium,modifier=Modifier.then(Modifier.fillMaxWidth().wrapContentWidth()).padding(horizontal=12.dp))
      }
      }
    }
    Spacer(Modifier.height(28.dp))
    OutlinedTextField(username,{username=it},placeholder={Text(if(loginRole=="Owner")"Owner ID" else "Employee ID",color=Color(0xFFD0C3C7))},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=OutlinedTextFieldDefaults.colors(focusedTextColor=Color.White,unfocusedTextColor=Color.White,focusedContainerColor=Color(0xFF49343A),unfocusedContainerColor=Color(0xFF49343A),focusedBorderColor=Color(0xFFB00035),unfocusedBorderColor=Color.Transparent,cursorColor=Color.White))
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(password,{password=it},placeholder={Text("Password",color=Color(0xFFD0C3C7))},singleLine=true,visualTransformation=if(passwordVisible)androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),trailingIcon={TextButton(onClick={passwordVisible=!passwordVisible}){Text(if(passwordVisible)"HIDE" else "SHOW",color=Color.White)}},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=OutlinedTextFieldDefaults.colors(focusedTextColor=Color.White,unfocusedTextColor=Color.White,focusedContainerColor=Color(0xFF49343A),unfocusedContainerColor=Color(0xFF49343A),focusedBorderColor=Color(0xFFB00035),unfocusedBorderColor=Color.Transparent,cursorColor=Color.White))
    Spacer(Modifier.height(28.dp))
    Button(enabled=!busy&&username.isNotBlank()&&password.isNotBlank(),onClick={busy=true;message="Signing in…";scope.launch{try{val s=withContext(Dispatchers.IO){ApiClient.login(username,password)};if((loginRole=="Owner")!=(s.role=="owner")){message="This account does not have $loginRole access."}else{session=s;authPrefs.edit().putString("refresh_token",s.refreshToken).apply();message="Signed in securely.";if(s.role!="owner")loadSites(s)}}catch(e:Exception){message=e.message?: "Sign in failed."}finally{busy=false}}},modifier=Modifier.fillMaxWidth().height(58.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFB00035),disabledContainerColor=Color(0xFF6B3445))){Text(if(busy)"Please wait…" else "Login",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}
    Spacer(Modifier.height(22.dp))
    TextButton(onClick={message="For password reset, please contact the MK Electricals administrator."}){Text("Forgot Password?",color=Color.White,style=MaterialTheme.typography.titleMedium)}
   }else{
    Text("Hello, ${session?.name}",modifier=Modifier.fillMaxWidth(),color=Color.White,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
    Spacer(Modifier.height(18.dp))
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(26.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFFDF9F6))){
     Column(Modifier.padding(20.dp)){
      if(session?.role=="owner") { OwnerDashboard(session!!,onSignOut={authPrefs.edit().clear().apply();session=null;sites=emptyList();selected=null;password="";message="Signed out."}) } else {
       Text("Today's attendance",color=Deep,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
       Text(if(tracking)"● LIVE LOCATION ACTIVE" else "○ Not clocked in",color=if(tracking)Color(0xFF16834A)else Wine,fontWeight=FontWeight.Bold)
       if(sites.isNotEmpty()) { var expanded by remember{mutableStateOf(false)}; ExposedDropdownMenuBox(expanded=expanded,onExpandedChange={expanded=it}){OutlinedTextField(selected?.name?: "Choose site",{},readOnly=true,label={Text("Assigned site")},modifier=Modifier.menuAnchor().fillMaxWidth());ExposedDropdownMenu(expanded=expanded,onDismissRequest={expanded=false}){sites.forEach{site->DropdownMenuItem(text={Text(site.name)},onClick={selected=site;expanded=false})}}} }
       Button(enabled=!busy&&selected!=null,onClick={if(!tracking){val fine=ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;val background=Build.VERSION.SDK_INT<29||ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_BACKGROUND_LOCATION)==PackageManager.PERMISSION_GRANTED;if(!fine){val p=mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION);if(Build.VERSION.SDK_INT>=33)p.add(Manifest.permission.POST_NOTIFICATIONS);permission.launch(p.toTypedArray())}else if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){permission.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))}else if(!background){showBackgroundPrompt=true}else{busy=true;scope.launch{try{val loc=withContext(Dispatchers.IO){locationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,null).await()}?:throw Exception("Unable to get GPS location. Turn on Location and try again.");val site=selected!!;val distance=FloatArray(1);android.location.Location.distanceBetween(loc.latitude,loc.longitude,site.lat,site.lng,distance);if(distance[0]>site.radius)throw Exception("You are ${distance[0].toInt()} m away. Clock-in allowed only within ${site.radius} m of ${site.name}.");val s=session!!;val id=withContext(Dispatchers.IO){ApiClient.clockIn(s.accessToken,s.userId,site,loc.latitude,loc.longitude)};attendanceId=id;val intent=Intent(context,LocationTrackingService::class.java).putExtra("token",s.accessToken).putExtra("refreshToken",s.refreshToken).putExtra("uid",s.userId).putExtra("attendance",id).putExtra("site",site.id).putExtra("siteLat",site.lat).putExtra("siteLng",site.lng).putExtra("radius",site.radius);ContextCompat.startForegroundService(context,intent);tracking=true;message="Clocked in at ${site.name}. Live GPS tracking is on."}catch(e:Exception){message=e.message?:"Clock-in failed."}finally{busy=false}}}}else{busy=true;scope.launch{try{val s=session!!;val id=attendanceId!!;val loc=try{withContext(Dispatchers.IO){locationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,null).await()}}catch(_:Exception){null};withContext(Dispatchers.IO){ApiClient.clockOut(s.accessToken,id,loc?.latitude,loc?.longitude)};context.startService(Intent(context,LocationTrackingService::class.java).setAction("STOP"));tracking=false;attendanceId=null;message=if(loc==null)"Clocked out. GPS was unavailable, so the clock-out location was not saved. Tracking stopped."else"Clocked out. Location tracking stopped."}catch(e:Exception){message=e.message?:"Clock-out failed."}finally{busy=false}}}},modifier=Modifier.fillMaxWidth().padding(top=14.dp)){Text(if(busy)"Please wait…" else if(tracking)"Clock out" else "Clock in")}
       OutlinedButton(enabled=!tracking&&!busy,onClick={authPrefs.edit().clear().apply();session=null;sites=emptyList();selected=null;attendanceId=null;password="";message="Signed out."},modifier=Modifier.fillMaxWidth()){Text("Sign out")}
       WorkerFieldTools(session!!)
      }
     }
    }
   }
   Spacer(Modifier.height(18.dp))
   Text(message,color=Color(0xFFF0DDE3),style=MaterialTheme.typography.bodySmall)
   Spacer(Modifier.height(10.dp))
   Text("Secure • Site-aware • Built for field teams",color=Color(0xFFE9D8DE),style=MaterialTheme.typography.bodySmall)
  }
 }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OwnerDashboard(session:ApiClient.Session,onSignOut:()->Unit){
 val scope= rememberCoroutineScope()
 var employees by remember{mutableStateOf(emptyList<org.json.JSONObject>())}
 var sites by remember{mutableStateOf(emptyList<org.json.JSONObject>())}
 var locations by remember{mutableStateOf(emptyList<org.json.JSONObject>())};var attendance by remember{mutableStateOf(emptyList<org.json.JSONObject>())}
 var worker by remember{mutableStateOf("")};var site by remember{mutableStateOf("")}
 var name by remember{mutableStateOf("")};var address by remember{mutableStateOf("")}
 var lat by remember{mutableStateOf("")};var lng by remember{mutableStateOf("")};var radius by remember{mutableStateOf("150")}
 var task by remember{mutableStateOf("")};var details by remember{mutableStateOf("")}
 var material by remember{mutableStateOf("")};var quantity by remember{mutableStateOf("")};var unit by remember{mutableStateOf("pcs")};var materialAction by remember{mutableStateOf("issued")}
 var status by remember{mutableStateOf("Loading owner dashboard…")};var busy by remember{mutableStateOf(false)}
 fun refresh(){scope.launch{busy=true;try{
  val result=withContext(Dispatchers.IO){listOf(ApiClient.ownerEmployees(session.accessToken),ApiClient.ownerSites(session.accessToken),ApiClient.ownerLiveLocations(session.accessToken),ApiClient.ownerRecentAttendance(session.accessToken))}
  employees=(0 until result[0].length()).map{result[0].getJSONObject(it)}
  sites=(0 until result[1].length()).map{result[1].getJSONObject(it)}
  locations=(0 until result[2].length()).map{result[2].getJSONObject(it)}
  attendance=(0 until result[3].length()).map{result[3].getJSONObject(it)}
  if(worker.isBlank())worker=employees.firstOrNull{it.optString("role")=="worker"}?.optString("id").orEmpty()
  if(site.isBlank())site=sites.firstOrNull()?.optString("id").orEmpty()
  status="Dashboard updated"
 }catch(e:Exception){status=e.message?: "Could not load owner data"}finally{busy=false}}}
 LaunchedEffect(session.userId){ while(true){ refresh(); delay(30_000L) } }
 Column(Modifier.fillMaxWidth().heightIn(max=620.dp).verticalScroll(rememberScrollState()).padding(vertical=4.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
  Text("OWNER DASHBOARD",color=Deep,fontWeight=FontWeight.Black,style=MaterialTheme.typography.titleLarge)
  OutlinedButton(onClick=onSignOut,modifier=Modifier.fillMaxWidth()){Text("Sign out")}
  Text("Employees: ${employees.size}  •  Sites: ${sites.size}  •  Live: ${locations.size}",color=Wine)
  Text("EMPLOYEE DIRECTORY",color=Deep,fontWeight=FontWeight.Bold)
  if(employees.isEmpty()) Text("No employees found.",color=Wine)
  employees.filter{it.optString("role")=="worker"}.forEach{person->
   val active=person.optBoolean("is_active")
   Text("${person.optString("full_name","Employee")} • ${person.optString("employee_code","No code")}\n${person.optString("designation","Field staff")} • ${if(active)"Active" else "Inactive"}",color=Deep,style=MaterialTheme.typography.bodySmall)
  }
  HorizontalDivider()
  Text("SITE DIRECTORY",color=Deep,fontWeight=FontWeight.Bold)
  if(sites.isEmpty()) Text("No sites created yet.",color=Wine)
  sites.forEach{place->
   Text("${place.optString("name","Site")} • ${if(place.optBoolean("is_active"))"Active" else "Inactive"}\n${place.optString("address","Address not added")} • ${place.optInt("geofence_radius_m",150)} m geofence",color=Deep,style=MaterialTheme.typography.bodySmall)
  }
  OutlinedButton(onClick={refresh()},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("Refresh dashboard")}
  Text("LIVE EMPLOYEE LOCATIONS",color=Deep,fontWeight=FontWeight.Bold)
  if(locations.isEmpty())Text("No live employee locations yet.",color=Wine)
  locations.forEach{item->
   val person=item.optJSONObject("profiles");val place=item.optJSONObject("sites")
   Text("${person?.optString("full_name")?:"Employee"} • ${place?.optString("name")?:"Site"}\n${item.optDouble("latitude")}, ${item.optDouble("longitude")} • ±${item.optInt("accuracy_m")} m",color=Deep,style=MaterialTheme.typography.bodySmall)
  }
  HorizontalDivider()
  Text("RECENT ATTENDANCE",color=Deep,fontWeight=FontWeight.Bold)
  if(attendance.isEmpty())Text("No attendance records found.",color=Wine)
  attendance.forEach{row->
   val person=row.optJSONObject("profiles");val place=row.optJSONObject("sites")
   val started=row.optString("clock_in").replace("T"," ").take(16)
   val rawEnd=row.optString("clock_out")
   val ended=if(rawEnd=="null"||rawEnd.isBlank())"Still clocked in" else rawEnd.replace("T"," ").take(16)
   val duration=runCatching{val start=java.time.Instant.parse(row.optString("clock_in"));val finish=if(rawEnd=="null"||rawEnd.isBlank())java.time.Instant.now() else java.time.Instant.parse(rawEnd);val minutes=java.time.Duration.between(start,finish).toMinutes().coerceAtLeast(0);"${minutes/60}h ${minutes%60}m"}.getOrDefault("—")
   Text("${person?.optString("full_name")?:"Employee"} • ${place?.optString("name")?:"Site"}\n$started → $ended • $duration",color=Deep,style=MaterialTheme.typography.bodySmall)
  }
  HorizontalDivider()
  Text("CREATE SITE",color=Deep,fontWeight=FontWeight.Bold)
  OutlinedTextField(name,{name=it},label={Text("Site name")},singleLine=true,modifier=Modifier.fillMaxWidth())
  OutlinedTextField(address,{address=it},label={Text("Address")},modifier=Modifier.fillMaxWidth())
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   OutlinedTextField(lat,{lat=it},label={Text("Latitude")},modifier=Modifier.weight(1f))
   OutlinedTextField(lng,{lng=it},label={Text("Longitude")},modifier=Modifier.weight(1f))
  }
  OutlinedTextField(radius,{radius=it},label={Text("Geofence radius in metres")},singleLine=true,modifier=Modifier.fillMaxWidth())
  Button(enabled=!busy&&name.isNotBlank()&&lat.toDoubleOrNull()!=null&&lng.toDoubleOrNull()!=null&&(radius.toIntOrNull()?:0) in 20..2000,onClick={
   scope.launch{busy=true;try{withContext(Dispatchers.IO){ApiClient.createSite(session.accessToken,name,address,lat.toDouble(),lng.toDouble(),radius.toInt())};name="";address="";status="Site created";refresh()}catch(e:Exception){status=e.message?:"Site creation failed"}finally{busy=false}}
  },modifier=Modifier.fillMaxWidth()){Text("Create site")}
  HorizontalDivider()
  Text("ASSIGN EMPLOYEE TO SITE",color=Deep,fontWeight=FontWeight.Bold)
  var workerOpen by remember{mutableStateOf(false)}
  ExposedDropdownMenuBox(expanded=workerOpen,onExpandedChange={workerOpen=it}){
   OutlinedTextField(employees.firstOrNull{it.optString("id")==worker}?.optString("full_name")?:"Choose employee",{},readOnly=true,label={Text("Employee")},modifier=Modifier.menuAnchor().fillMaxWidth())
   ExposedDropdownMenu(expanded=workerOpen,onDismissRequest={workerOpen=false}){employees.filter{it.optString("role")=="worker"&&it.optBoolean("is_active")}.forEach{p->DropdownMenuItem(text={Text(p.optString("full_name"))},onClick={worker=p.optString("id");workerOpen=false})}}
  }
  var siteOpen by remember{mutableStateOf(false)}
  ExposedDropdownMenuBox(expanded=siteOpen,onExpandedChange={siteOpen=it}){
   OutlinedTextField(sites.firstOrNull{it.optString("id")==site}?.optString("name")?:"Choose site",{},readOnly=true,label={Text("Site")},modifier=Modifier.menuAnchor().fillMaxWidth())
   ExposedDropdownMenu(expanded=siteOpen,onDismissRequest={siteOpen=false}){sites.forEach{p->DropdownMenuItem(text={Text(p.optString("name"))},onClick={site=p.optString("id");siteOpen=false})}}
  }
  Button(enabled=!busy&&worker.isNotBlank()&&site.isNotBlank(),onClick={scope.launch{busy=true;try{withContext(Dispatchers.IO){ApiClient.assignWorker(session.accessToken,worker,site)};status="Employee assigned"}catch(e:Exception){status=e.message?:"Assignment failed"}finally{busy=false}}},modifier=Modifier.fillMaxWidth()){Text("Assign employee")}
  HorizontalDivider()
  Text("ASSIGN DAILY TASK",color=Deep,fontWeight=FontWeight.Bold)
  OutlinedTextField(task,{task=it},label={Text("Task title")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(details,{details=it},label={Text("Task details")},modifier=Modifier.fillMaxWidth())
  Button(enabled=!busy&&worker.isNotBlank()&&task.isNotBlank(),onClick={scope.launch{busy=true;try{withContext(Dispatchers.IO){ApiClient.createTask(session.accessToken,worker,site.ifBlank{null},task,details)};task="";details="";status="Task assigned"}catch(e:Exception){status=e.message?:"Task assignment failed"}finally{busy=false}}},modifier=Modifier.fillMaxWidth()){Text("Assign task to selected employee")}
  HorizontalDivider()
  Text("MATERIAL ISSUE / RETURN",color=Deep,fontWeight=FontWeight.Bold)
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   FilterChip(selected=materialAction=="issued",onClick={materialAction="issued"},label={Text("Issue")})
   FilterChip(selected=materialAction=="returned",onClick={materialAction="returned"},label={Text("Return")})
  }
  OutlinedTextField(material,{material=it},label={Text("Material name")},modifier=Modifier.fillMaxWidth())
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   OutlinedTextField(quantity,{quantity=it},label={Text("Quantity")},modifier=Modifier.weight(1f))
   OutlinedTextField(unit,{unit=it},label={Text("Unit")},modifier=Modifier.weight(1f))
  }
  Button(enabled=!busy&&worker.isNotBlank()&&material.isNotBlank()&&(quantity.toDoubleOrNull()?:0.0)>0,onClick={scope.launch{busy=true;try{withContext(Dispatchers.IO){ApiClient.issueMaterial(session.accessToken,worker,site.ifBlank{null},material,quantity.toDouble(),unit,"",materialAction)};material="";quantity="";status=if(materialAction=="issued")"Material issue recorded" else "Material return recorded"}catch(e:Exception){status=e.message?:"Material transaction failed"}finally{busy=false}}},modifier=Modifier.fillMaxWidth()){Text(if(materialAction=="issued") "Record material issue" else "Record material return")}
  OwnerFieldTools(session, org.json.JSONArray().apply { employees.forEach { put(it) } }, org.json.JSONArray().apply { sites.forEach { put(it) } })
  Text(status,color=Wine,style=MaterialTheme.typography.bodySmall)
 }
}
