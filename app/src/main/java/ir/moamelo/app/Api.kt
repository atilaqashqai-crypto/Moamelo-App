package ir.moamelo.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ApiResult(val ok: Boolean, val json: JSONObject, val error: String?, val code: Int)

object Api {
    const val BASE = "https://moamelo.lizoo.ir/api"
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    suspend fun call(method: String, path: String, body: JSONObject? = null): ApiResult = withContext(Dispatchers.IO) {
        try {
            val builder = Request.Builder().url(BASE + path)
            val token = Session.token
            if (token != null) builder.header("Authorization", "Bearer $token")
            val rb = (body ?: JSONObject()).toString().toRequestBody(JSON_TYPE)
            when (method) {
                "POST" -> builder.post(rb)
                "PUT" -> builder.put(rb)
                "PATCH" -> builder.patch(rb)
                "DELETE" -> builder.delete()
                else -> builder.get()
            }
            client.newCall(builder.build()).execute().use { resp ->
                val text = resp.body?.string() ?: ""
                val json = try { JSONObject(text) } catch (e: Exception) { JSONObject() }
                if (resp.isSuccessful) {
                    ApiResult(true, json, null, resp.code)
                } else {
                    if (resp.code == 401 && token != null) Session.clear()
                    val msg = if (json.has("error")) json.optString("error") else "خطای ارتباط با سرور (${resp.code})"
                    ApiResult(false, json, msg, resp.code)
                }
            }
        } catch (e: Exception) {
            ApiResult(false, JSONObject(), "ارتباط با سرور برقرار نشد. اینترنت را بررسی کنید.", 0)
        }
    }
}
