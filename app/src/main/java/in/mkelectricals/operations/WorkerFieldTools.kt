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

@Composable
fun WorkerFieldTools(session: ApiClient.Session) {
    val scope = rememberCoroutineScope()
    var tasks by remember { mutableStateOf(emptyList<org.json.JSONObject>()) }
    var materials by remember { mutableStateOf(emptyList<org.json.JSONObject>()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Loading your tasks and issued materials…") }
    fun refresh() {
        scope.launch {
            busy = true
            try {
                val result = withContext(Dispatchers.IO) {
                    Pair(ApiClient.workerDailyTasks(session.accessToken, session.userId), ApiClient.workerMaterials(session.accessToken, session.userId))
                }
                tasks = (0 until result.first.length()).map { result.first.getJSONObject(it) }
                materials = (0 until result.second.length()).map { result.second.getJSONObject(it) }
                message = "Your records are up to date."
            } catch (e: Exception) { message = e.message ?: "Could not load your records." }
            finally { busy = false }
        }
    }
    LaunchedEffect(session.userId) { refresh() }
    Column(Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
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
        HorizontalDivider()
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
        OutlinedButton(onClick = { refresh() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Refresh tasks & materials") }
        Text(message, color = Color(0xFF641B32), style = MaterialTheme.typography.bodySmall)
    }
}
