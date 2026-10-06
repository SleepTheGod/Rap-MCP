package com.example.network

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotBlank() && !key.equals("MY_GEMINI_API_KEY", ignoreCase = true)
    }

    suspend fun generateContent(
        prompt: String,
        systemInstruction: String? = null,
        model: String = "gemini-3.5-flash"
    ): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!isConfigured()) {
            Log.d(TAG, "Gemini API key not configured, using built-in generator.")
            return@withContext null
        }

        try {
            val url = "$BASE_URL$model:generateContent?key=$apiKey"

            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            }

            val rootJson = JSONObject().apply {
                put("contents", contentsArray)
                if (!systemInstruction.isNullOrBlank()) {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", systemInstruction)
                            })
                        })
                    })
                }
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.85)
                    put("topP", 0.95)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = rootJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API failed with code ${response.code}: ${response.body?.string()}")
                return@withContext null
            }

            val responseStr = response.body?.string() ?: return@withContext null
            val respJson = JSONObject(responseStr)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val first = candidates.getJSONObject(0)
                val content = first.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return@withContext parts.getJSONObject(0).optString("text")
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini API: ${e.message}", e)
            null
        }
    }
}
