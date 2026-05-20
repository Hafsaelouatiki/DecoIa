package hafsa.elouatiki.decoia.api

import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.Gson
import hafsa.elouatiki.decoia.models.AnalysisResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

class GroqApiService {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiUrl = "https://api.groq.com/openai/v1/chat/completions"
    private val transcriptionsUrl = "https://api.groq.com/openai/v1/audio/transcriptions"

    suspend fun transcribeAudio(audioFile: File): String? = withContext(Dispatchers.IO) {
        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", audioFile.name, audioFile.asRequestBody("audio/mpeg".toMediaType()))
                .addFormDataPart("model", "whisper-large-v3-turbo")
                .addFormDataPart("language", "fr")
                .build()

            val request = Request.Builder()
                .url(transcriptionsUrl)
                .addHeader("Authorization", "Bearer ${ApiKeys.GROQ_API_KEY}")
                .post(requestBody).build()

            val responseBody = client.newCall(request).execute().body?.string() ?: ""
            val root = gson.fromJson(responseBody, Map::class.java)
            return@withContext root["text"] as? String
        } catch (e: Exception) { null }
    }

    suspend fun getChatResponse(userMessage: String): String? = withContext(Dispatchers.IO) {
        try {
            val requestBodyJson = mapOf(
                "model" to "llama-3.3-70b-versatile",
                "messages" to listOf(
                    mapOf("role" to "system", "content" to "Tu es DecoIA, expert en décoration. Sois concis."),
                    mapOf("role" to "user", "content" to userMessage)
                )
            )
            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer ${ApiKeys.GROQ_API_KEY}")
                .post(gson.toJson(requestBodyJson).toRequestBody("application/json".toMediaType())).build()

            val responseBody = client.newCall(request).execute().body?.string() ?: ""
            val root = gson.fromJson(responseBody, Map::class.java)
            val choices = root["choices"] as? List<*>
            val message = (choices?.get(0) as? Map<*, *>)?.get("message") as? Map<*, *>
            return@withContext message?.get("content") as? String
        } catch (e: Exception) { null }
    }

    suspend fun analyzeRoom(bitmap: Bitmap): AnalysisResponse? = withContext(Dispatchers.IO) {
        try {
            val base64Image = bitmapToBase64(bitmap)
            val promptText = "Analyse cette photo et réponds UNIQUEMENT avec un JSON : {\"style_actuel\": \"string\", \"couleurs_actuelles\": [\"c1\"], \"type_piece\": \"string\", \"recommandations\": {\"style_cible\": \"string\", \"palette_couleurs\": [\"#hex\"], \"mobilier\": [{\"nom\": \"string\", \"recherche\": \"string\"}], \"prompt_image\": \"string\"}}"

            val requestBodyJson = mapOf(
                "model" to "llama-3.2-11b-vision-preview",
                "messages" to listOf(
                    mapOf("role" to "user", "content" to listOf(
                        mapOf("type" to "text", "text" to promptText),
                        mapOf("type" to "image_url", "image_url" to mapOf("url" to "data:image/jpeg;base64,$base64Image"))
                    ))
                ),
                "response_format" to mapOf("type" to "json_object")
            )
            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer ${ApiKeys.GROQ_API_KEY}")
                .post(gson.toJson(requestBodyJson).toRequestBody("application/json".toMediaType())).build()

            val responseBody = client.newCall(request).execute().body?.string() ?: ""
            return@withContext gson.fromJson(responseBody, Map::class.java)?.let { root ->
                val choices = root["choices"] as? List<*>
                val content = ((choices?.get(0) as? Map<*, *>)?.get("message") as? Map<*, *>)?.get("content") as? String
                gson.fromJson(content, AnalysisResponse::class.java)
            }
        } catch (e: Exception) { null }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
}
