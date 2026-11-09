package `in`.mkelectricals.operations

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE = "https://ljijisvtevblzfnzpctm.supabase.co"
    private const val KEY = "sb_publishable_9c3U7xh7A8HLN6RjzQN_JA_yAUNr6Da"
    private val http = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
    data class Session(val accessToken:String,val refreshToken:String,val userId:String,val role:String,val name:String)
    data class Site(val id:String,val name:String,val lat:Double,val lng:Double,val radius:Int)

    private fun request(path:String, token:String?=null, method:String="GET", body:String?=null, prefer:String?=null):String {
        val b=Request.Builder().url(BASE+path).addHeader("apikey",KEY).addHeader("Accept","application/json")
        if(token!=null)b.addHeader("Authorization","Bearer $token")
        if(prefer!=null)b.addHeader("Prefer",prefer)
        if(body!=null)b.addHeader("Content-Type","application/json")
        val rb=body?.toRequestBody("application/json".toMediaType())
        b.method(method, if(method=="GET"||method=="HEAD") null else rb ?: ByteArray(0).toRequestBody(null))
        http.newCall(b.build()).execute().use { response ->
            val text=response.body?.string().orEmpty()
            if(!response.isSuccessful) throw Exception(JSONObject(text.ifBlank{"{}"}).optString("message",JSONObject(text.ifBlank{"{}"}).optString("msg","Request failed (${response.code})")))
            return text
        }
    }
    fun login(username:String,password:String):Session {
        val email=username.trim().lowercase()+"@staff.mk-electricals.in"
        val auth=JSONObject().put("email",email).put("password",password)
        val data=JSONObject(request("/auth/v1/token?grant_type=password",method="POST",body=auth.toString()))
        val token=data.getString("access_token"); val uid=data.getJSONObject("user").getString("id")
        val rows=JSONArray(request("/rest/v1/profiles?select=id,full_name,role,is_active&id=eq.$uid&limit=1",token))
        if(rows.length()==0) throw Exception("Employee profile not found.")
        val p=rows.getJSONObject(0); if(!p.optBoolean("is_active")) throw Exception("This account is inactive. Contact the owner.")
        return Session(token,data.optString("refresh_token"),uid,p.optString("role"),p.optString("full_name",username))
    }
    fun assignedSites(token:String,uid:String):List<Site>{
        val rows=JSONArray(request("/rest/v1/site_assignments?select=site_id&worker_id=eq.$uid&is_active=eq.true",token))
        val out=mutableListOf<Site>()
        for(i in 0 until rows.length()){
            val id=rows.getJSONObject(i).getString("site_id")
            val s=JSONArray(request("/rest/v1/sites?select=id,name,latitude,longitude,geofence_radius_m&id=eq.$id&is_active=eq.true&limit=1",token))
            if(s.length()>0){val x=s.getJSONObject(0);if(!x.isNull("latitude")&&!x.isNull("longitude"))out.add(Site(id,x.optString("name"),x.getDouble("latitude"),x.getDouble("longitude"),x.optInt("geofence_radius_m",150)))}
        }
        return out
    }
    fun activeAttendance(token:String,uid:String):JSONObject? {
        val rows=JSONArray(request("/rest/v1/attendance?select=id,site_id,clock_in&worker_id=eq.$uid&clock_out=is.null&order=clock_in.desc&limit=1",token))
        return if(rows.length()>0)rows.getJSONObject(0) else null
    }
    fun clockIn(token:String,uid:String,site:Site,lat:Double,lng:Double):String {
        val body=JSONObject().put("worker_id",uid).put("site_id",site.id).put("clock_in_lat",lat).put("clock_in_lng",lng)
        val rows=JSONArray(request("/rest/v1/attendance",token,"POST",body.toString(),"return=representation"))
        return rows.getJSONObject(0).getString("id")
    }
    fun clockOut(token:String,attendanceId:String,lat:Double?=null,lng:Double?=null){
        val body=JSONObject().put("clock_out",java.time.Instant.now().toString())
        if(lat!=null&&lng!=null)body.put("clock_out_lat",lat).put("clock_out_lng",lng)
        request("/rest/v1/attendance?id=eq.$attendanceId",token,"PATCH",body.toString(),"return=minimal")
    }
    fun ownerEmployees(token:String):JSONArray = JSONArray(request("/rest/v1/profiles?select=id,employee_code,full_name,username,designation,is_active,role,joining_date,photo_path,id_serial,verification_code&order=created_at.desc",token))
    fun ownerSites(token:String):JSONArray = JSONArray(request("/rest/v1/sites?select=id,name,address,latitude,longitude,geofence_radius_m,is_active&order=created_at.desc",token))
    fun ownerLiveLocations(token:String):JSONArray {
        val cutoff=java.time.Instant.now().minusSeconds(90).toString()
        return JSONArray(request("/rest/v1/worker_live_locations?select=worker_id,site_id,latitude,longitude,accuracy_m,recorded_at,profiles(full_name,employee_code),sites(name)&recorded_at=gte.$cutoff&order=recorded_at.desc",token))
    }
    fun ownerRecentAttendance(token:String):JSONArray = JSONArray(request("/rest/v1/attendance?select=id,worker_id,site_id,clock_in,clock_out,profiles(full_name,employee_code),sites(name)&order=clock_in.desc&limit=30",token))
    fun assignWorker(token:String,workerId:String,siteId:String){
        val body=JSONObject().put("worker_id",workerId).put("site_id",siteId).put("is_active",true)
        request("/rest/v1/site_assignments?on_conflict=site_id,worker_id",token,"POST",body.toString(),"resolution=merge-duplicates,return=minimal")
    }
    fun createTask(token:String,workerId:String,siteId:String?,title:String,description:String){
        val body=JSONObject().put("worker_id",workerId).put("title",title).put("description",description).put("work_date",java.time.LocalDate.now().toString()).put("status","planned")
        if(siteId!=null)body.put("site_id",siteId)
        request("/rest/v1/daily_tasks",token,"POST",body.toString(),"return=minimal")
    }
    fun issueMaterial(token:String,workerId:String,siteId:String?,name:String,qty:Double,unit:String,notes:String,transactionType:String="issued"){
        require(transactionType in setOf("issued","returned"))
        val body=JSONObject().put("worker_id",workerId).put("material_name",name).put("quantity",qty).put("unit",unit).put("transaction_type",transactionType).put("notes",notes)
        if(siteId!=null)body.put("site_id",siteId)
        request("/rest/v1/material_transactions",token,"POST",body.toString(),"return=minimal")
    }
    fun createSite(token:String,name:String,address:String,lat:Double,lng:Double,radius:Int){
        val body=JSONObject().put("name",name).put("address",address).put("latitude",lat).put("longitude",lng).put("geofence_radius_m",radius).put("is_active",true)
        request("/rest/v1/sites",token,"POST",body.toString(),"return=minimal")
    }
    fun refreshAccessToken(refreshToken:String):Session {
        val body=JSONObject().put("refresh_token",refreshToken)
        val data=JSONObject(request("/auth/v1/token?grant_type=refresh_token",method="POST",body=body.toString()))
        val token=data.getString("access_token")
        val uid=data.getJSONObject("user").getString("id")
        val rows=JSONArray(request("/rest/v1/profiles?select=id,full_name,role,is_active&id=eq.$uid&limit=1",token))
        if(rows.length()==0) throw Exception("Employee profile not found.")
        val p=rows.getJSONObject(0)
        if(!p.optBoolean("is_active")) throw Exception("This account is inactive.")
        return Session(token,data.optString("refresh_token",refreshToken),uid,p.optString("role"),p.optString("full_name"))
    }
    fun pushLocation(token:String,uid:String,attendanceId:String,siteId:String,lat:Double,lng:Double,accuracy:Float){
        val body=JSONObject().put("worker_id",uid).put("attendance_id",attendanceId).put("site_id",siteId)
            .put("latitude",lat).put("longitude",lng).put("accuracy_m",accuracy).put("recorded_at",java.time.Instant.now().toString()).put("updated_at",java.time.Instant.now().toString())
        request("/rest/v1/worker_live_locations?on_conflict=worker_id",token,"POST",body.toString(),"resolution=merge-duplicates,return=minimal")
    }

    fun workerDailyTasks(token:String,uid:String):JSONArray =
        JSONArray(request("/rest/v1/daily_tasks?select=id,site_id,work_date,title,description,status,hours,sites(name)&worker_id=eq.$uid&order=work_date.desc,created_at.desc",token))

    fun updateTaskStatus(token:String,taskId:String,status:String) {
        require(status in setOf("planned","in_progress","completed","blocked")) { "Invalid task status" }
        request("/rest/v1/daily_tasks?id=eq.$taskId",token,"PATCH",JSONObject().put("status",status).toString(),"return=minimal")
    }

    fun workerMaterials(token:String,uid:String):JSONArray =
        JSONArray(request("/rest/v1/material_transactions?select=id,site_id,material_name,quantity,unit,transaction_type,transaction_date,notes,sites(name)&worker_id=eq.$uid&order=transaction_date.desc",token))

    fun workerToolCustody(token:String,uid:String):JSONArray =
        JSONArray(request("/rest/v1/tool_custody?select=id,tool_name,serial_number,site_id,issued_at,returned_at,condition_out,condition_in,notes,sites(name)&assigned_to=eq.$uid&order=issued_at.desc",token))

    fun ownerRoomSurvey(token:String,siteId:String?=null):JSONArray {
        val filter=if(siteId.isNullOrBlank()) "" else "&site_id=eq.$siteId"
        return JSONArray(request("/rest/v1/room_survey_entries?select=id,site_id,room_name,item_description,quantity,unit,notes,created_by,created_at,sites(name),profiles!room_survey_entries_created_by_fkey(full_name)&order=created_at.desc$filter",token))
    }

    fun createRoomSurvey(token:String,uid:String,siteId:String,room:String,item:String,quantity:Double,unit:String,notes:String) {
        require(quantity>0.0 && room.isNotBlank() && item.isNotBlank())
        val body=JSONObject().put("site_id",siteId).put("room_name",room.trim()).put("item_description",item.trim())
            .put("quantity",quantity).put("unit",unit.trim().ifBlank { "Nos" }).put("created_by",uid)
        if(notes.isNotBlank()) body.put("notes",notes.trim())
        request("/rest/v1/room_survey_entries",token,"POST",body.toString(),"return=minimal")
    }

    fun ownerToolCustody(token:String):JSONArray =
        JSONArray(request("/rest/v1/tool_custody?select=id,tool_name,serial_number,assigned_to,site_id,issued_at,returned_at,condition_out,condition_in,notes,profiles!tool_custody_assigned_to_fkey(full_name,employee_code),sites(name)&order=issued_at.desc",token))

    fun issueTool(token:String,uid:String,workerId:String,siteId:String?,toolName:String,serial:String,condition:String,notes:String) {
        require(workerId.isNotBlank() && toolName.isNotBlank())
        val body=JSONObject().put("created_by",uid).put("assigned_to",workerId).put("tool_name",toolName.trim())
        if(siteId!=null) body.put("site_id",siteId)
        if(serial.isNotBlank()) body.put("serial_number",serial.trim())
        if(condition.isNotBlank()) body.put("condition_out",condition.trim())
        if(notes.isNotBlank()) body.put("notes",notes.trim())
        request("/rest/v1/tool_custody",token,"POST",body.toString(),"return=minimal")
    }

    fun returnTool(token:String,id:String,condition:String,notes:String) {
        val body=JSONObject().put("returned_at",java.time.Instant.now().toString())
        if(condition.isNotBlank()) body.put("condition_in",condition.trim())
        if(notes.isNotBlank()) body.put("notes",notes.trim())
        request("/rest/v1/tool_custody?id=eq.$id&returned_at=is.null",token,"PATCH",body.toString(),"return=minimal")
    }


    fun employeeProfile(token:String,uid:String):JSONObject? {
        val rows=JSONArray(request("/rest/v1/profiles?select=id,employee_code,full_name,username,role,is_active,designation,joining_date,photo_path,residential_address,id_serial,verification_code&id=eq.$uid&limit=1",token))
        return if(rows.length()>0) rows.getJSONObject(0) else null
    }


    fun verifyEmployee(token:String?=null,code:String):JSONObject? {
        val safe=java.net.URLEncoder.encode(code.trim(),"UTF-8")
        val rows=JSONArray(request("/rest/v1/employee_verifications?select=verification_code,employee_id,full_name,designation,is_active,updated_at&verification_code=eq.$safe&limit=1",token))
        return if(rows.length()>0) rows.getJSONObject(0) else null
    }

}
