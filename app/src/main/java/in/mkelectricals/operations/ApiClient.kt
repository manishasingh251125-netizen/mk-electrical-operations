package in.mkelectricals.operations

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
    fun clockIn(token:String,uid:String,site:Site,lat:Double,lng:Double):String {
        val body=JSONObject().put("worker_id",uid).put("site_id",site.id).put("clock_in_lat",lat).put("clock_in_lng",lng)
        val rows=JSONArray(request("/rest/v1/attendance",token,"POST",body.toString(),"return=representation"))
        return rows.getJSONObject(0).getString("id")
    }
    fun clockOut(token:String,attendanceId:String,lat:Double,lng:Double){
        val body=JSONObject().put("clock_out",java.time.Instant.now().toString()).put("clock_out_lat",lat).put("clock_out_lng",lng)
        request("/rest/v1/attendance?id=eq.$attendanceId",token,"PATCH",body.toString(),"return=minimal")
    }
    fun ownerEmployees(token:String):JSONArray = JSONArray(request("/rest/v1/profiles?select=id,employee_code,full_name,username,designation,is_active,role&order=created_at.desc",token))
    fun ownerSites(token:String):JSONArray = JSONArray(request("/rest/v1/sites?select=id,name,address,latitude,longitude,geofence_radius_m,is_active&order=created_at.desc",token))
    fun ownerLiveLocations(token:String):JSONArray = JSONArray(request("/rest/v1/worker_live_locations?select=worker_id,site_id,latitude,longitude,accuracy_m,recorded_at,profiles(full_name,employee_code),sites(name)&order=recorded_at.desc",token))
    fun createTask(token:String,workerId:String,siteId:String?,title:String,description:String){
        val body=JSONObject().put("worker_id",workerId).put("title",title).put("description",description).put("work_date",java.time.LocalDate.now().toString()).put("status","planned")
        if(siteId!=null)body.put("site_id",siteId)
        request("/rest/v1/daily_tasks",token,"POST",body.toString(),"return=minimal")
    }
    fun issueMaterial(token:String,workerId:String,siteId:String?,name:String,qty:Double,unit:String,notes:String){
        val body=JSONObject().put("worker_id",workerId).put("material_name",name).put("quantity",qty).put("unit",unit).put("transaction_type","issued").put("notes",notes)
        if(siteId!=null)body.put("site_id",siteId)
        request("/rest/v1/material_transactions",token,"POST",body.toString(),"return=minimal")
    }
    fun createSite(token:String,name:String,address:String,lat:Double,lng:Double,radius:Int){
        val body=JSONObject().put("name",name).put("address",address).put("latitude",lat).put("longitude",lng).put("geofence_radius_m",radius).put("is_active",true)
        request("/rest/v1/sites",token,"POST",body.toString(),"return=minimal")
    }
    fun pushLocation(token:String,uid:String,attendanceId:String,siteId:String,lat:Double,lng:Double,accuracy:Float){
        val body=JSONObject().put("worker_id",uid).put("attendance_id",attendanceId).put("site_id",siteId)
            .put("latitude",lat).put("longitude",lng).put("accuracy_m",accuracy).put("recorded_at",java.time.Instant.now().toString()).put("updated_at",java.time.Instant.now().toString())
        request("/rest/v1/worker_live_locations?on_conflict=worker_id",token,"POST",body.toString(),"resolution=merge-duplicates,return=minimal")
    }
}
