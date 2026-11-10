package `in`.mkelectricals.operations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerFieldTools(session: ApiClient.Session) {
    val scope = rememberCoroutineScope()
    var tasks by remember { mutableStateOf(emptyList<org.json.JSONObject>()) }
    var materials by remember { mutableStateOf(emptyList<org.json.JSONObject>()) }
    var tools by remember { mutableStateOf(emptyList<org.json.JSONObject>()) }
    var profile by remember { mutableStateOf<org.json.JSONObject?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Loading your tasks and issued materials…") }
    var section by remember { mutableStateOf("Tasks") }
    fun refresh() {
        scope.launch {
            busy = true
            try {
                val result = withContext(Dispatchers.IO) {
                    val taskRows = ApiClient.workerDailyTasks(session.accessToken, session.userId)
                    val materialRows = ApiClient.workerMaterials(session.accessToken, session.userId)
                    val toolRows = ApiClient.workerToolCustody(session.accessToken, session.userId)
                    val profileRow = ApiClient.employeeProfile(session.accessToken, session.userId)
                    arrayOf(taskRows, materialRows, toolRows, profileRow)
                }
                val taskRows = result[0] as JSONArray
                val materialRows = result[1] as JSONArray
                val toolRows = result[2] as JSONArray
                tasks = (0 until taskRows.length()).map { taskRows.getJSONObject(it) }
                materials = (0 until materialRows.length()).map { materialRows.getJSONObject(it) }
                tools = (0 until toolRows.length()).map { toolRows.getJSONObject(it) }
                profile = result[3] as org.json.JSONObject?
                message = "Your records are up to date."
            } catch (e: Exception) { message = e.message ?: "Could not load your records." }
            finally { busy = false }
        }
    }
    LaunchedEffect(session.userId) { refresh() }
    Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("YOUR WORKSPACE", color = Color(0xFF35101F), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("Tasks", "Materials", "Tools", "My ID").forEach { item ->
                FilterChip(selected = section == item, onClick = { section = item }, label = { Text(item, maxLines = 1) })
            }
        }
        if (section == "My ID") {
        Text("MY DIGITAL STAFF ID", color = Color(0xFF35101F), style = MaterialTheme.typography.titleMedium)
        profile?.let { p ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(p.optString("full_name"), style = MaterialTheme.typography.titleLarge)
                    Text("Employee code: ${p.optString("employee_code", "Pending")}")
                    Text(p.optString("designation", "Field staff"))
                    Text("ID serial: ${p.optString("id_serial", "Pending")}", style = MaterialTheme.typography.bodySmall)
                    Text(if (p.optBoolean("is_active")) "ACTIVE EMPLOYEE" else "INACTIVE EMPLOYEE", color = if (p.optBoolean("is_active")) Color(0xFF16834A) else Color.Red)
                    Text("Verification code: ${p.optString("verification_code", "Pending")}", style = MaterialTheme.typography.bodySmall)
                    Text("This code can be checked against the company verification register.", style = MaterialTheme.typography.bodySmall)
                }
            }
        } ?: Text("Employee ID details are not available yet.", style = MaterialTheme.typography.bodySmall)
        }
        if (section == "Tasks") {
        Text("MY DAILY TASKS", color = Color(0xFF35101F), style = MaterialTheme.typography.titleMedium)
        if (tasks.isEmpty()) Text("No tasks assigned yet.", style = MaterialTheme.typography.bodySmall)
        tasks.take(20).forEach { task ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(task.optString("title"), style = MaterialTheme.typography.titleSmall)
                    Text("${task.optJSONObject("sites")?.optString("name") ?: "General task"} • ${task.optString("work_date")}", style = MaterialTheme.typography.bodySmall)
                    if (task.optString("description").isNotBlank() && task.optString("description") != "null") Text(task.optString("description"), style = MaterialTheme.typography.bodySmall)
                    var expanded by remember(task.optString("id")) { mutableStateOf(false) }
                    val current = task.optString("status", "planned")
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(current.replace('_',' ').replaceFirstChar { it.uppercase() }, {}, readOnly = true, label = { Text("Update progress") }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("planned", "in_progress", "completed", "blocked").forEach { next ->
                                DropdownMenuItem(text = { Text(next.replace('_',' ').replaceFirstChar { it.uppercase() }) }, onClick = {
                                    expanded = false
                                    scope.launch {
                                        busy = true
                                        try { withContext(Dispatchers.IO) { ApiClient.updateTaskStatus(session.accessToken, task.optString("id"), next) }; task.put("status", next); message = "Task progress updated." }
                                        catch (e: Exception) { message = e.message ?: "Could not update task." }
                                        finally { busy = false }
                                    }
                                })
                            }
                        }
                    }
                }
            }
        }
        }
        if (section == "Materials") {
        Text("MY ISSUED MATERIALS", color = Color(0xFF35101F), style = MaterialTheme.typography.titleMedium)
        if (materials.isEmpty()) Text("No material issues recorded.", style = MaterialTheme.typography.bodySmall)
        materials.take(25).forEach { row ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text(row.optString("material_name"), style = MaterialTheme.typography.titleSmall)
                    Text("${row.optString("quantity")} ${row.optString("unit")} • ${row.optString("transaction_type")} • ${row.optString("transaction_date").take(10)}", style = MaterialTheme.typography.bodySmall)
                    val note = row.optString("notes")
                    if (note.isNotBlank() && note != "null") Text(note, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        }
        if (section == "Tools") {
        Text("MY TOOL CUSTODY", color = Color(0xFF35101F), style = MaterialTheme.typography.titleMedium)
        if (tools.isEmpty()) Text("No tools have been issued to you.", style = MaterialTheme.typography.bodySmall)
        tools.take(25).forEach { tool ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(tool.optString("tool_name"), style = MaterialTheme.typography.titleSmall)
                    val serial = tool.optString("serial_number")
                    if (serial.isNotBlank() && serial != "null") Text("Serial: $serial", style = MaterialTheme.typography.bodySmall)
                    Text("Site: " + (tool.optJSONObject("sites")?.optString("name") ?: "Not specified"), style = MaterialTheme.typography.bodySmall)
                    val returned = tool.optString("returned_at")
                    Text("Issued: " + tool.optString("issued_at").take(10) + " • " + if (returned == "null" || returned.isBlank()) "With you" else "Returned: " + returned.take(10), style = MaterialTheme.typography.bodySmall)
                    val condition = tool.optString("condition_out")
                    if (condition.isNotBlank() && condition != "null") Text("Condition at issue: $condition", style = MaterialTheme.typography.bodySmall)
                    val returnedCondition = tool.optString("condition_in")
                    if (returnedCondition.isNotBlank() && returnedCondition != "null") Text("Return condition: $returnedCondition", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        }
        OutlinedButton(onClick = { refresh() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Refreshing…" else "Refresh records") }
        Text(message, color = Color(0xFF641B32), style = MaterialTheme.typography.bodySmall)
    }
}
