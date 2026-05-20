package hafsa.elouatiki.decoia

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class RoomAnalysisService {
    private val TAG = "IA_DEBUG"
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiUrl = "https://api.groq.com/openai/v1/chat/completions"

    suspend fun analyzeRoom(bitmap: Bitmap): AnalysisResponse? = withContext(Dispatchers.IO) {
        try {
            val base64Image = bitmapToBase64(bitmap)
            val promptText = "Tu es un expert décorateur d'intérieur. Analyse cette photo et réponds UNIQUEMENT avec un JSON valide : {\"style_actuel\": \"string\", \"couleurs_actuelles\": [\"c1\"], \"type_piece\": \"salon/chambre/cuisine\", \"recommandations\": {\"style_cible\": \"moderne\", \"palette_couleurs\": [\"#hex\"], \"mobilier\": [{\"nom\": \"canapé\", \"recherche\": \"canapé moderne\"}], \"prompt_image\": \"A professional interior design photo of a luxury decorated room, same structure, 8k\"}}"

            val requestBodyJson = mapOf(
                "model" to "meta-llama/llama-4-maverick-17b-128e-instruct",                "messages" to listOf(
                    mapOf(
                        "role" to "user",
                        "content" to listOf(
                            mapOf(
                                "type" to "image_url",
                                "image_url" to mapOf(
                                    "url" to "data:image/jpeg;base64,$base64Image"
                                )
                            ),
                            mapOf(
                                "type" to "text",
                                "text" to promptText
                            )
                        )
                    )
                ),
                "max_tokens" to 1024
            )

            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer ${ApiKeys.GROQ_API_KEY}")
                .addHeader("Content-Type", "application/json")
                .post(gson.toJson(requestBodyJson).toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Erreur API Groq : ${response.code} - $responseBody")
                return@withContext null
            }

            val root = gson.fromJson(responseBody, Map::class.java)
            val choices = root["choices"] as? List<*>
            val firstChoice = choices?.get(0) as? Map<*, *>
            val message = firstChoice?.get("message") as? Map<*, *>
            val textResponse = message?.get("content") as? String ?: ""

            Log.d(TAG, "Réponse Groq : $textResponse")

            val jsonMatch = Regex("""\{[\s\S]*\}""").find(textResponse)
            val cleanJson = jsonMatch?.value ?: return@withContext null
            return@withContext gson.fromJson(cleanJson, AnalysisResponse::class.java)

        } catch (e: Exception) {
            Log.e(TAG, "Erreur Service : ${e.message}")
            null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        val maxSide = 1024
        val ratio = Math.min(
            maxSide.toFloat() / bitmap.width,
            maxSide.toFloat() / bitmap.height
        )
        val scaled = if (ratio < 1f) Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt(),
            (bitmap.height * ratio).toInt(),
            true
        ) else bitmap
        scaled.compress(Bitmap.CompressFormat.JPEG, 60, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
}