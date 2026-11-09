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
fun OwnerFieldTools(session: ApiClient.Session, employees: JSONArray, sites: JSONArray) {
    val scope = rememberCoroutineScope()
    val workers = remember(employees) { (0 until employees.length()).map { employees.getJSONObject(it) }.filter { it.optString("role") == "worker" && it.optBoolean("is_active") } }
    val places = remember(sites) { (0 until sites.length()).map { sites.getJSONObject(it) }.filter { it.optBoolean("is_active") } }
    var workerId by remember(workers) { mutableStateOf(workers.firstOrNull()?.optString("id").orEmpty()) }
    var siteId by remember(places) { mutableStateOf(places.firstOrNull()?.optString("id").orEmpty()) }
    var workerMenu by remember { mutableStateOf(false) }
    var siteMenu by remember { mutableStateOf(false) }
    var room by remember { mutableStateOf("") }
    var item by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("Nos") }
    var surveyNotes by remember { mutableStateOf("") }
    var tool by remember { mutableStateOf("") }
    var serial by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("Good") }
    var toolNotes by remember { mutableStateOf("") }
    var surveyRows by remember { mutableStateOf(emptyList<org.json.JSONObject>()) }
    var custodyRows by remember { mutableStateOf(emptyList<org.json.JSONObject>()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("BOQ surveys and tool custody are saved to the company database.") }

    fun refresh() {
        scope.launch {
            busy = true
            try {
                val result = withContext(Dispatchers.IO) {
                    Pair(ApiClient.ownerRoomSurvey(session.accessToken, siteId.ifBlank { null }), ApiClient.ownerToolCustody(session.accessToken))
                }
                surveyRows = (0 until result.first.length()).map { result.first.getJSONObject(it) }
                custodyRows = (0 until result.second.length()).map { result.second.getJSONObject(it) }
                message = "Records refreshed."
            } catch (e: Exception) { message = e.message ?: "Could not refresh records." }
            finally { busy = false }
        }
    }
    LaunchedEffect(session.userId, siteId) { refresh() }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        HorizontalDivider()
        Text("BOQ / ROOM SURVEY", color = Color(0xFF35101F), style = MaterialTheme.typography.titleMedium)
        ExposedDropdownMenuBox(expanded = siteMenu, onExpandedChange = { siteMenu = it }) {
            OutlinedTextField(places.firstOrNull { it.optString("id") == siteId }?.optString("name") ?: "Choose site", {}, readOnly = true, label = { Text("Site") }, modifier = Modifier.menuAnchor().fillMaxWidth())
            ExposedDropdownMenu(expanded = siteMenu, onDismissRequest = { siteMenu = false }) {
                places.forEach { p -> DropdownMenuItem(text = { Text(p.optString("name")) }, onClick = { siteId = p.optString("id"); siteMenu = false }) }
            }
        }
        OutlinedTextField(room, { room = it }, label = { Text("Room / area") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(item, { item = it }, label = { Text("Electrical item / work description") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(qty, { qty = it }, label = { Text("Quantity") }, modifier = Modifier.weight(1f))
            OutlinedTextField(unit, { unit = it }, label = { Text("Unit") }, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(surveyNotes, { surveyNotes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())
        Button(enabled = !busy && siteId.isNotBlank() && room.isNotBlank() && item.isNotBlank() && (qty.toDoubleOrNull() ?: 0.0) > 0, onClick = {
            scope.launch {
                busy = true
                try {
                    withContext(Dispatchers.IO) { ApiClient.createRoomSurvey(session.accessToken, session.userId, siteId, room, item, qty.toDouble(), unit, surveyNotes) }
                    room = ""; item = ""; qty = "1"; surveyNotes = ""; message = "BOQ survey entry saved."; refresh()
                } catch (e: Exception) { message = e.message ?: "Could not save survey." }
                finally { busy = false }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Save BOQ entry") }
        Text("Recent survey entries", style = MaterialTheme.typography.titleSmall)
        surveyRows.take(12).forEach { row ->
            Text("• ${row.optString("room_name")} — ${row.optString("item_description")}: ${row.optString("quantity")} ${row.optString("unit")}", style = MaterialTheme.typography.bodySmall)
        }

        HorizontalDivider()
        Text("TOOL / METER CUSTODY", color = Color(0xFF35101F), style = MaterialTheme.typography.titleMedium)
        ExposedDropdownMenuBox(expanded = workerMenu, onExpandedChange = { workerMenu = it }) {
            OutlinedTextField(workers.firstOrNull { it.optString("id") == workerId }?.optString("full_name") ?: "Choose employee", {}, readOnly = true, label = { Text("Assign to employee") }, modifier = Modifier.menuAnchor().fillMaxWidth())
            ExposedDropdownMenu(expanded = workerMenu, onDismissRequest = { workerMenu = false }) {
                workers.forEach { p -> DropdownMenuItem(text = { Text(p.optString("full_name")) }, onClick = { workerId = p.optString("id"); workerMenu = false }) }
            }
        }
        OutlinedTextField(tool, { tool = it }, label = { Text("Tool / meter name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(serial, { serial = it }, label = { Text("Serial number (optional)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(condition, { condition = it }, label = { Text("Condition at issue") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(toolNotes, { toolNotes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())
        Button(enabled = !busy && workerId.isNotBlank() && tool.isNotBlank(), onClick = {
            scope.launch {
                busy = true
                try {
                    withContext(Dispatchers.IO) { ApiClient.issueTool(session.accessToken, session.userId, workerId, siteId.ifBlank { null }, tool, serial, condition, toolNotes) }
                    tool = ""; serial = ""; toolNotes = ""; message = "Tool issue recorded."; refresh()
                } catch (e: Exception) { message = e.message ?: "Could not issue tool." }
                finally { busy = false }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Record tool issue") }
        Text("Tool register", style = MaterialTheme.typography.titleSmall)
        custodyRows.take(15).forEach { row ->
            val returned = !row.isNull("returned_at") && row.optString("returned_at").isNotBlank()
            val assignee = row.optJSONObject("profiles")?.optString("full_name") ?: "Employee"
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${row.optString("tool_name")} • $assignee", style = MaterialTheme.typography.bodyMedium)
                    Text("Serial: ${row.optString("serial_number", "—")} • ${if (returned) "Returned" else "Issued"}", style = MaterialTheme.typography.bodySmall)
                    if (!returned) {
                        OutlinedButton(enabled = !busy, onClick = {
                            scope.launch {
                                busy = true
                                try { withContext(Dispatchers.IO) { ApiClient.returnTool(session.accessToken, row.optString("id"), "Returned", "") }; message = "Tool return recorded."; refresh() }
                                catch (e: Exception) { message = e.message ?: "Could not record return." }
                                finally { busy = false }
                            }
                        }) { Text("Mark returned") }
                    }
                }
            }
        }
        OutlinedButton(onClick = { refresh() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Refresh BOQ & tool register") }
        Text(message, color = Color(0xFF641B32), style = MaterialTheme.typography.bodySmall)
    }
}
