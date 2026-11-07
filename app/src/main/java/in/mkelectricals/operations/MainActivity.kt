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

private val Wine = Color(0xFF641B32)
private val DeepWine = Color(0xFF35101F)
private val Gold = Color(0xFFC49A58)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Wine, secondary = Gold, background = DeepWine)) { AppScreen(
            onStart = { startTracking() }, onStop = { stopService(Intent(this, LocationTrackingService::class.java)) }
        ) } }
    }
    private fun startTracking() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine) ContextCompat.startForegroundService(this, Intent(this, LocationTrackingService::class.java))
    }
}

@Composable
private fun AppScreen(onStart: () -> Unit, onStop: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var signedIn by remember { mutableStateOf(false) }
    var tracking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Sign in with your company username and password") }
    val infinite = rememberInfiniteTransition(label = "orb")
    val rotation by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "rotation")
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) { onStart(); tracking = true; message = "Location tracking is active for this shift." }
        else message = "Location permission is required to clock in."
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(DeepWine, Wine, Color(0xFF160B12))))) {
        Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(150.dp).graphicsLayer { rotationZ = rotation; cameraDistance = 18 * density }.shadow(22.dp, RoundedCornerShape(38.dp)).background(Brush.linearGradient(listOf(Gold, Wine, Color(0xFF9C5268))), RoundedCornerShape(38.dp)), contentAlignment = Alignment.Center) {
                Text("MK", color = Color.White, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(22.dp))
            Text("MK ELECTRICALS", color = Gold, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(if (signedIn) "Shift dashboard" else "Operations", color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF9F6)), shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(22.dp)) {
                    if (!signedIn) {
                        Text("Employee sign in", color = DeepWine, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        OutlinedTextField(username, { username = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Button(onClick = { message = "Authentication connection is being wired to the company account."; signedIn = true }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("Sign in") }
                    } else {
                        Text("Attendance", color = DeepWine, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(if (tracking) "● LIVE LOCATION ON" else "○ Not clocked in", color = if (tracking) Color(0xFF16834A) else Wine, fontWeight = FontWeight.Bold)
                        Button(onClick = {
                            if (!tracking) {
                                val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                permissionLauncher.launch(perms.toTypedArray())
                            } else { onStop(); tracking = false; message = "Tracking stopped. Clock-out saved locally." }
                        }, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) { Text(if (tracking) "Clock out" else "Clock in") }
                        OutlinedButton(onClick = { signedIn = false; tracking = false; onStop() }, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(message, color = Color(0xFF6D5961), style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Secure • Site-aware • Built for field teams", color = Color(0xFFE9D8DE), style = MaterialTheme.typography.bodySmall)
        }
    }
}
