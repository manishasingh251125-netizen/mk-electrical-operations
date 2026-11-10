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
import androidx.compose.foundation.clickable
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
 var username by remember{mutableStateOf("")};var password by remember{mutableStateOf("")};var loginRole by remember{mutableStateOf("employee")};var passwordVisible by remember{mutableStateOf(false)}
 var session by remember{mutableStateOf<ApiClient.Session?>(null)}
 var sites by remember{mutableStateOf(emptyList<ApiClient.Site>())};var selected by remember{mutableStateOf<ApiClient.Site?>(null)}
 var attendanceId by remember{mutableStateOf<String?>(null)};var busy by remember{mutableStateOf(false)}
 var message by remember{mutableStateOf("Sign in with your company username and password")}
 var tracking by remember{mutableStateOf(false)};var showBackgroundPrompt by remember{mutableStateOf(false)}
 val locationClient= remember{LocationServices.getFusedLocationProviderClient(context)}
 val authPrefs=remember{context.getSharedPreferences("mk_auth_session",android.content.Context.MODE_PRIVATE)}
 val pulse= rememberInfiniteTransition(label="pulse");val rotation by pulse.animateFloat(0f,360f,infiniteRepeatable(tween(16000,easing=LinearEasing)),label="spin")
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
  Column(Modifier.fillMaxSize().padding(horizontal=16.dp, vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Top){
   if(session==null){ Spacer(Modifier.height(34.dp)); MkBrandLogo(); Spacer(Modifier.height(36.dp)) }
   if(session==null){
    Column(Modifier.fillMaxWidth().background(Color(0xFF21070F).copy(alpha=0.62f),RoundedCornerShape(28.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
     Text("Sign In",color=Color.White,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
     Row(Modifier.fillMaxWidth().background(Color(0xFF17050B),RoundedCornerShape(40.dp)).padding(4.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){
      listOf("employee","owner").forEach { role ->
       val active=loginRole==role
       Box(Modifier.weight(1f).background(if(active)Color(0xFFB10D3B) else Color.Transparent,RoundedCornerShape(36.dp)).clickable { loginRole=role },contentAlignment=Alignment.Center){
        Text(if(role=="employee")"Employee" else "Owner",color=Color.White,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(vertical=13.dp))
       }
      }
     }
     OutlinedTextField(username,{username=it},label={Text(if(loginRole=="owner")"Owner ID" else "Employee ID")},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=OutlinedTextFieldDefaults.colors(focusedTextColor=Color.White,unfocusedTextColor=Color.White,focusedBorderColor=Color(0xFFCE3159),unfocusedBorderColor=Color(0xFF72505A),focusedLabelColor=Color(0xFFFFB2C4),unfocusedLabelColor=Color(0xFFD7C2C8),cursorColor=Color.White))
     OutlinedTextField(password,{password=it},label={Text("Password")},singleLine=true,visualTransformation=if(passwordVisible)androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),trailingIcon={TextButton(onClick={passwordVisible=!passwordVisible}){Text(if(passwordVisible)"HIDE" else "SHOW",color=Color(0xFFFFB2C4))}},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=OutlinedTextFieldDefaults.colors(focusedTextColor=Color.White,unfocusedTextColor=Color.White,focusedBorderColor=Color(0xFFCE3159),unfocusedBorderColor=Color(0xFF72505A),focusedLabelColor=Color(0xFFFFB2C4),unfocusedLabelColor=Color(0xFFD7C2C8),cursorColor=Color.White))
     Button(enabled=!busy&&username.isNotBlank()&&password.isNotBlank(),onClick={busy=true;message="Signing in…";scope.launch{try{val s=withContext(Dispatchers.IO){ApiClient.login(username,password)};if(s.role!=loginRole)throw Exception(if(loginRole=="owner")"This account is not an Owner account. Select Employee or use the correct Owner ID." else "This account is not an Employee account. Select Owner or use the correct Employee ID.");authPrefs.edit().putString("refresh_token",s.refreshToken).apply();session=s;message="Signed in securely.";if(s.role!="owner")loadSites(s)}catch(e:Exception){message=e.message?: "Sign in failed."}finally{busy=false}}},modifier=Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFB10D3B),disabledContainerColor=Color(0xFF6B3344))){Text(if(busy)"Please wait…" else "Login",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)}
     TextButton(onClick={message="For password assistance, please contact your MK Electricals administrator."},modifier=Modifier.align(Alignment.CenterHorizontally)){Text("Forgot Password?",color=Color.White)}
    }
   }else if(session?.role=="owner"){
    OwnerDashboard(session!!,onSignOut={authPrefs.edit().clear().apply();session=null;sites=emptyList();selected=null;password="";message="Signed out."})
   }else{
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(26.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFFDF9F6))){
     Column(Modifier.padding(16.dp)){
       Text("Today’s attendance",color=Deep,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
       Text(if(tracking)"● LIVE LOCATION ACTIVE" else "○ Not clocked in",color=if(tracking)Color(0xFF16834A)else Wine,fontWeight=FontWeight.Bold)
       if(sites.isNotEmpty()) { var expanded by remember{mutableStateOf(false)}; ExposedDropdownMenuBox(expanded=expanded,onExpandedChange={expanded=it}){OutlinedTextField(selected?.name?: "Choose site",{},readOnly=true,label={Text("Assigned site")},modifier=Modifier.menuAnchor().fillMaxWidth());ExposedDropdownMenu(expanded=expanded,onDismissRequest={expanded=false}){sites.forEach{site->DropdownMenuItem(text={Text(site.name)},onClick={selected=site;expanded=false})}}} }
       Button(enabled=!busy&&selected!=null,onClick={if(!tracking){val fine=ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;val background=Build.VERSION.SDK_INT<29||ContextCompat.checkSelfPermission(context,Manifest.permission.ACCESS_BACKGROUND_LOCATION)==PackageManager.PERMISSION_GRANTED;if(!fine){val p=mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION);if(Build.VERSION.SDK_INT>=33)p.add(Manifest.permission.POST_NOTIFICATIONS);permission.launch(p.toTypedArray())}else if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){permission.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))}else if(!background){showBackgroundPrompt=true}else{busy=true;scope.launch{try{val loc=withContext(Dispatchers.IO){locationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,null).await()};if(loc==null)throw Exception("Unable to get GPS location. Turn on Location and try again.");val site=selected!!;val distance=FloatArray(1);android.location.Location.distanceBetween(loc.latitude,loc.longitude,site.lat,site.lng,distance);if(distance[0]>site.radius)throw Exception("You are ${distance[0].toInt()} m away. Clock-in allowed only within ${site.radius} m of ${site.name}.");val s=session!!;val id=withContext(Dispatchers.IO){ApiClient.clockIn(s.accessToken,s.userId,site,loc.latitude,loc.longitude)};attendanceId=id;val intent=Intent(context,LocationTrackingService::class.java).putExtra("token",s.accessToken).putExtra("refreshToken",s.refreshToken).putExtra("uid",s.userId).putExtra("attendance",id).putExtra("site",site.id).putExtra("siteLat",site.lat).putExtra("siteLng",site.lng).putExtra("radius",site.radius);ContextCompat.startForegroundService(context,intent);tracking=true;message="Clocked in at ${site.name}. Live GPS tracking is on."}catch(e:Exception){message=e.message?: "Clock-in failed."}finally{busy=false}}}}else{busy=true;scope.launch{try{val s=session!!;val id=attendanceId!!;val loc=try{withContext(Dispatchers.IO){locationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,null).await()}}catch(_:Exception){null};withContext(Dispatchers.IO){ApiClient.clockOut(s.accessToken,id,loc?.latitude,loc?.longitude)};context.startService(Intent(context,LocationTrackingService::class.java).setAction("STOP"));tracking=false;attendanceId=null;message=if(loc==null)"Clocked out. GPS unavailable." else "Clocked out. Tracking stopped."}catch(e:Exception){message=e.message?: "Clock-out failed."}finally{busy=false}}}},modifier=Modifier.fillMaxWidth().padding(top=14.dp)){Text(if(busy)"Please wait…" else if(tracking)"Clock out" else "Clock in")}
       OutlinedButton(enabled=!tracking&&!busy,onClick={authPrefs.edit().clear().apply();session=null;sites=emptyList();selected=null;attendanceId=null;password="";message="Signed out."},modifier=Modifier.fillMaxWidth()){Text("Sign out")}
       WorkerFieldTools(session!!)
     }
    }
   }
   if(session==null){ Spacer(Modifier.height(12.dp));Text(message,color=Color(0xFFEAD9DE),style=MaterialTheme.typography.bodySmall);Spacer(Modifier.weight(1f));Text("SECURE  •  SITE-AWARE  •  BUILT FOR FIELD TEAMS",color=Color(0xFFE9D8DE),style=MaterialTheme.typography.labelSmall) }
  }
 }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OwnerDashboard(session:ApiClient.Session,onSignOut:()->Unit){
 val scope=rememberCoroutineScope()
 var employees by remember{mutableStateOf(emptyList<org.json.JSONObject>())}
 var sites by remember{mutableStateOf(emptyList<org.json.JSONObject>())}
 var locations by remember{mutableStateOf(emptyList<org.json.JSONObject>())}
 var attendance by remember{mutableStateOf(emptyList<org.json.JSONObject>())}
 var worker by remember{mutableStateOf("")};var site by remember{mutableStateOf("")}
 var siteName by remember{mutableStateOf("")};var address by remember{mutableStateOf("")};var lat by remember{mutableStateOf("")};var lng by remember{mutableStateOf("")};var radius by remember{mutableStateOf("150")}
 var taskTitle by remember{mutableStateOf("")};var taskDetails by remember{mutableStateOf("")}
 var material by remember{mutableStateOf("")};var quantity by remember{mutableStateOf("")};var unit by remember{mutableStateOf("pcs")};var action by remember{mutableStateOf("issued")}
 var page by remember{mutableStateOf("Home")};var status by remember{mutableStateOf("Loading company data…")};var busy by remember{mutableStateOf(false)}
 fun refresh(){scope.launch{busy=true;try{val r=withContext(Dispatchers.IO){listOf(ApiClient.ownerEmployees(session.accessToken),ApiClient.ownerSites(session.accessToken),ApiClient.ownerLiveLocations(session.accessToken),ApiClient.ownerRecentAttendance(session.accessToken))};employees=(0 until r[0].length()).map{r[0].getJSONObject(it)};sites=(0 until r[1].length()).map{r[1].getJSONObject(it)};locations=(0 until r[2].length()).map{r[2].getJSONObject(it)};attendance=(0 until r[3].length()).map{r[3].getJSONObject(it)};if(worker.isBlank())worker=employees.firstOrNull{it.optString("role")=="worker"}?.optString("id").orEmpty();if(site.isBlank())site=sites.firstOrNull()?.optString("id").orEmpty();status="Updated just now"}catch(e:Exception){status=e.message?:"Could not load company data"}finally{busy=false}}}
 LaunchedEffect(session.userId){refresh();while(true){delay(30000L);refresh()}}
 val workers=employees.filter{it.optString("role")=="worker"}
 Column(Modifier.fillMaxWidth().heightIn(max=820.dp).verticalScroll(rememberScrollState()).background(Color(0xFF260811),RoundedCornerShape(24.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
   Column{Text("GOOD "+if(java.time.LocalTime.now().hour<12)"MORNING" else if(java.time.LocalTime.now().hour<17)"AFTERNOON" else "EVENING",color=Color(0xFFFFB5C8),style=MaterialTheme.typography.labelSmall);Text(session.name,color=Color.White,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("OWNER • MK ELECTRICALS",color=Color(0xFFE8C8D1),style=MaterialTheme.typography.labelSmall)}
   TextButton(onClick=onSignOut){Text("SIGN OUT",color=Color.White)}
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){OwnerMetric("STAFF",workers.size.toString(),Modifier.weight(1f));OwnerMetric("SITES",sites.size.toString(),Modifier.weight(1f));OwnerMetric("LIVE",locations.size.toString(),Modifier.weight(1f))}
  when(page){
   "Home"->{
    Text("Operations",color=Color.White,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
    OwnerNavGrid{page=it}
    OwnerPanel("LIVE TEAM",locations.size.toString()+" location updates"){
     if(locations.isEmpty())Text("No recent live GPS updates.",color=Deep)
     locations.take(8).forEach{v->val p=v.optJSONObject("profiles");val s=v.optJSONObject("sites");Text((p?.optString("full_name")?:"Employee")+" • "+(s?.optString("name")?:"Site"),color=Deep,fontWeight=FontWeight.SemiBold);Text(v.optDouble("latitude").toString()+" , "+v.optDouble("longitude").toString()+"  ±"+v.optInt("accuracy_m")+" m",color=Wine,style=MaterialTheme.typography.bodySmall)}
    }
    OwnerPanel("RECENT ATTENDANCE","Latest shift activity"){
     if(attendance.isEmpty())Text("No attendance records yet.",color=Deep)
     attendance.take(8).forEach{v->val p=v.optJSONObject("profiles");val s=v.optJSONObject("sites");val out=v.optString("clock_out");Text((p?.optString("full_name")?:"Employee")+" • "+(s?.optString("name")?:"Site"),color=Deep,fontWeight=FontWeight.SemiBold);Text(v.optString("clock_in").replace("T"," ").take(16)+" → "+if(out=="null"||out.isBlank())"On duty" else out.replace("T"," ").take(16),color=Wine,style=MaterialTheme.typography.bodySmall)}
    }
   }
   "Sites"->{
    OwnerPageTitle("Sites","Work locations and geofence settings")
    sites.forEach{v->OwnerPanel(v.optString("name"),v.optString("address")+" • "+v.optInt("geofence_radius_m",150)+" m radius"){Text(if(v.optBoolean("is_active"))"ACTIVE" else "INACTIVE",color=Wine)}}
    OwnerPanel("ADD SITE","Create a geofenced work location"){
     OutlinedTextField(siteName,{siteName=it},label={Text("Site name")},modifier=Modifier.fillMaxWidth())
     OutlinedTextField(address,{address=it},label={Text("Address")},modifier=Modifier.fillMaxWidth())
     Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(lat,{lat=it},label={Text("Latitude")},modifier=Modifier.weight(1f));OutlinedTextField(lng,{lng=it},label={Text("Longitude")},modifier=Modifier.weight(1f))}
     OutlinedTextField(radius,{radius=it},label={Text("Geofence metres")},modifier=Modifier.fillMaxWidth())
     Button(enabled=!busy&&siteName.isNotBlank()&&lat.toDoubleOrNull()!=null&&lng.toDoubleOrNull()!=null&&(radius.toIntOrNull()?:0) in 20..2000,onClick={scope.launch{busy=true;try{withContext(Dispatchers.IO){ApiClient.createSite(session.accessToken,siteName,address,lat.toDouble(),lng.toDouble(),radius.toInt())};siteName="";address="";status="Site created";refresh()}catch(e:Exception){status=e.message?:"Site creation failed"}finally{busy=false}}},modifier=Modifier.fillMaxWidth()){Text("CREATE SITE")}
    }
   }
   "Dispatch"->{
    OwnerPageTitle("Dispatch","Assign work and manage material movement")
    OwnerPanel("EMPLOYEE","Select the team member"){
     var expanded by remember{mutableStateOf(false)}
     ExposedDropdownMenuBox(expanded=expanded,onExpandedChange={expanded=it}){OutlinedTextField(workers.firstOrNull{it.optString("id")==worker}?.optString("full_name")?:"Choose employee",{},readOnly=true,label={Text("Employee")},modifier=Modifier.menuAnchor().fillMaxWidth());ExposedDropdownMenu(expanded=expanded,onDismissRequest={expanded=false}){workers.filter{it.optBoolean("is_active")}.forEach{p->DropdownMenuItem(text={Text(p.optString("full_name"))},onClick={worker=p.optString("id");expanded=false})}}}
     var siteExpanded by remember{mutableStateOf(false)}
     ExposedDropdownMenuBox(expanded=siteExpanded,onExpandedChange={siteExpanded=it}){OutlinedTextField(sites.firstOrNull{it.optString("id")==site}?.optString("name")?:"Choose site",{},readOnly=true,label={Text("Site")},modifier=Modifier.menuAnchor().fillMaxWidth());ExposedDropdownMenu(expanded=siteExpanded,onDismissRequest={siteExpanded=false}){sites.forEach{p->DropdownMenuItem(text={Text(p.optString("name"))},onClick={site=p.optString("id");siteExpanded=false})}}}
    }
    OwnerPanel("DAILY TASK","Send instructions to the selected employee"){
     OutlinedTextField(taskTitle,{taskTitle=it},label={Text("Task title")},modifier=Modifier.fillMaxWidth())
     OutlinedTextField(taskDetails,{taskDetails=it},label={Text("Instructions")},modifier=Modifier.fillMaxWidth())
     Button(enabled=!busy&&worker.isNotBlank()&&taskTitle.isNotBlank(),onClick={scope.launch{busy=true;try{withContext(Dispatchers.IO){ApiClient.createTask(session.accessToken,worker,site.ifBlank{null},taskTitle,taskDetails)};taskTitle="";taskDetails="";status="Task assigned"}catch(e:Exception){status=e.message?:"Task assignment failed"}finally{busy=false}}},modifier=Modifier.fillMaxWidth()){Text("ASSIGN TASK")}
    }
    OwnerPanel("MATERIAL ISSUE / RETURN","Record issued and returned stock"){
     Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=action=="issued",onClick={action="issued"},label={Text("Issue")});FilterChip(selected=action=="returned",onClick={action="returned"},label={Text("Return")})}
     OutlinedTextField(material,{material=it},label={Text("Material name")},modifier=Modifier.fillMaxWidth())
     Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(quantity,{quantity=it},label={Text("Quantity")},modifier=Modifier.weight(1f));OutlinedTextField(unit,{unit=it},label={Text("Unit")},modifier=Modifier.weight(1f))}
     Button(enabled=!busy&&worker.isNotBlank()&&material.isNotBlank()&&(quantity.toDoubleOrNull()?:0.0)>0,onClick={scope.launch{busy=true;try{withContext(Dispatchers.IO){ApiClient.issueMaterial(session.accessToken,worker,site.ifBlank{null},material,quantity.toDouble(),unit,"",action)};material="";quantity="";status="Material transaction saved"}catch(e:Exception){status=e.message?:"Material transaction failed"}finally{busy=false}}},modifier=Modifier.fillMaxWidth()){Text(if(action=="issued")"RECORD ISSUE" else "RECORD RETURN")}
    }
   }
   "Staff"->{
    OwnerPageTitle("Staff","Employee directory and status")
    if(workers.isEmpty())Text("No employees found.",color=Color.White)
    workers.forEach{p->OwnerPanel(p.optString("full_name"),p.optString("employee_code")+" • "+p.optString("designation")){Text(if(p.optBoolean("is_active"))"ACTIVE" else "INACTIVE",color=Wine)}}
   }
   "BOQ & Tools"->{
    OwnerPageTitle("BOQ / Tool Custody","Room surveys, ID verification and equipment")
    OwnerFieldTools(session,org.json.JSONArray().apply{employees.forEach{put(it)}},org.json.JSONArray().apply{sites.forEach{put(it)}})
   }
   "Reports"->{
    OwnerPageTitle("Reports & Insights","Attendance and field activity")
    attendance.forEach{v->val p=v.optJSONObject("profiles");val s=v.optJSONObject("sites");OwnerPanel(p?.optString("full_name")?:"Employee",s?.optString("name")?:"Site"){Text(v.optString("clock_in").replace("T"," ").take(16)+" → "+v.optString("clock_out").replace("T"," ").take(16),color=Deep)}}
   }
  }
  Text(status,color=Color(0xFFFFC2D0),style=MaterialTheme.typography.bodySmall)
  Row(Modifier.fillMaxWidth().background(Color(0xFF4A1022),RoundedCornerShape(18.dp)).padding(4.dp),horizontalArrangement=Arrangement.SpaceBetween){listOf("Home","Sites","Dispatch","Staff","BOQ & Tools","Reports").forEach{tab->TextButton(onClick={page=tab},modifier=Modifier.weight(1f)){Text(when(tab){"BOQ & Tools"->"BOQ";else->tab},color=if(page==tab)Color(0xFFFFB5C8)else Color.White,style=MaterialTheme.typography.labelSmall,maxLines=1)}}}
 }
}
@Composable private fun OwnerMetric(label:String,value:String,modifier:Modifier=Modifier){Card(modifier,colors=CardDefaults.cardColors(containerColor=Color(0xFFFDF9F6)),shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(10.dp)){Text(value,color=Deep,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text(label,color=Wine,style=MaterialTheme.typography.labelSmall)}}}
@Composable private fun OwnerPageTitle(title:String,subtitle:String){Text(title,color=Color.White,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text(subtitle,color=Color(0xFFE8C8D1),style=MaterialTheme.typography.bodySmall)}
@Composable private fun OwnerPanel(title:String,subtitle:String,content:@Composable ColumnScope.()->Unit){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFFDF9F6))){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(title,color=Deep,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleSmall);Text(subtitle,color=Wine,style=MaterialTheme.typography.bodySmall);content()}}}
@Composable private fun OwnerNavGrid(onPage:(String)->Unit){
 val items=listOf("Live Tracking" to "⌖","Sites" to "▦","Dispatch" to "⇧","Staff" to "♙","BOQ & Tools" to "▤","Reports" to "▥")
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)){items.chunked(2).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach{item->Card(Modifier.weight(1f).clickable{onPage(if(item.first=="Live Tracking")"Home" else item.first)},shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFFFDF9F6))){Column(Modifier.fillMaxWidth().padding(vertical=14.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(item.second,color=Wine,style=MaterialTheme.typography.headlineMedium);Text(item.first,color=Deep,fontWeight=FontWeight.SemiBold)}}}}}}
}
@Composable
private fun MkBrandLogo(){
 Column(horizontalAlignment=Alignment.CenterHorizontally){
  Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){
   Text("M",color=Color.White,style=MaterialTheme.typography.displayLarge,fontWeight=FontWeight.Black)
   Box(Modifier.width(8.dp).height(62.dp).padding(horizontal=1.dp),contentAlignment=Alignment.Center){Text("ϟ",color=Color(0xFFFFB5C8),style=MaterialTheme.typography.displayMedium,fontWeight=FontWeight.Black)}
   Text("K",color=Color.White,style=MaterialTheme.typography.displayLarge,fontWeight=FontWeight.Black)
  }
  Text("E L E C T R I C A L S",color=Color(0xFFE8D5D9),style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)
 }
}
