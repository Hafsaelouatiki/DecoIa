package hafsa.elouatiki.decoia

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
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
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    private val apiUrl = "https://api.groq.com/openai/v1/chat/completions"
    private val transcriptionsUrl = "https://api.groq.com/openai/v1/audio/transcriptions"

    suspend fun transcribeAudio(audioFile: File): String? = withContext(Dispatchers.IO) {
        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    audioFile.name,
                    audioFile.asRequestBody("audio/mpeg".toMediaType())
                )
                .addFormDataPart("model", "whisper-large-v3-turbo")
                .addFormDataPart("language", "fr")
                .build()

            val request = Request.Builder()
                .url(transcriptionsUrl)
                .addHeader("Authorization", "Bearer ${ApiKeys.GROQ_API_KEY}")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            Log.e("GroqApiService", "Transcription HTTP CODE = ${response.code}")
            Log.e("GroqApiService", "Transcription RESPONSE = $responseBody")

            if (!response.isSuccessful) return@withContext null

            val root = gson.fromJson(responseBody, Map::class.java)
            return@withContext root["text"] as? String

        } catch (e: Exception) {
            Log.e("GroqApiService", "Erreur transcription", e)
            null
        }
    }

    suspend fun getChatResponse(userMessage: String): String? = withContext(Dispatchers.IO) {
        try {
            val requestBodyJson = mapOf(
                "model" to "llama-3.3-70b-versatile",
                "messages" to listOf(
                    mapOf(
                        "role" to "system",
                        "content" to "Tu es DecoIA, expert en décoration. Sois concis."
                    ),
                    mapOf(
                        "role" to "user",
                        "content" to userMessage
                    )
                )
            )

            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer ${ApiKeys.GROQ_API_KEY}")
                .addHeader("Content-Type", "application/json")
                .post(
                    gson.toJson(requestBodyJson)
                        .toRequestBody("application/json".toMediaType())
                )
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            Log.e("GroqApiService", "Chat HTTP CODE = ${response.code}")
            Log.e("GroqApiService", "Chat RESPONSE = $responseBody")

            if (!response.isSuccessful) return@withContext null

            val root = gson.fromJson(responseBody, Map::class.java)
            val choices = root["choices"] as? List<*>
            val message = (choices?.getOrNull(0) as? Map<*, *>)?.get("message") as? Map<*, *>

            return@withContext message?.get("content") as? String

        } catch (e: Exception) {
            Log.e("GroqApiService", "Erreur chat", e)
            null
        }
    }

    suspend fun analyzeRoom(bitmap: Bitmap): AnalysisResponse? = withContext(Dispatchers.IO) {
        try {
            val base64Image = bitmapToBase64(bitmap)

            val promptText = """
                Analyse cette photo d'intérieur.

                Réponds uniquement avec un JSON valide.
                Ne mets aucun texte avant ou après.
                Ne mets pas de markdown.

                Format exact :
                {
                  "style_actuel": "string",
                  "couleurs_actuelles": ["string"],
                  "type_piece": "string",
                  "recommandations": {
                    "style_cible": "string",
                    "palette_couleurs": ["#FFFFFF"],
                    "mobilier": [
                      {
                        "nom": "string",
                        "recherche": "string"
                      }
                    ],
                    "promptImage": "string"
                  }
                }
            """.trimIndent()

            val requestBodyJson = mapOf(
                "model" to "meta-llama/llama-4-scout-17b-16e-instruct",
                "messages" to listOf(
                    mapOf(
                        "role" to "user",
                        "content" to listOf(
                            mapOf(
                                "type" to "text",
                                "text" to promptText
                            ),
                            mapOf(
                                "type" to "image_url",
                                "image_url" to mapOf(
                                    "url" to "data:image/jpeg;base64,$base64Image"
                                )
                            )
                        )
                    )
                ),
                "temperature" to 0.2,
                "max_completion_tokens" to 1024
            )

            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer ${ApiKeys.GROQ_API_KEY}")
                .addHeader("Content-Type", "application/json")
                .post(
                    gson.toJson(requestBodyJson)
                        .toRequestBody("application/json".toMediaType())
                )
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            Log.e("GroqApiService", "Analyse HTTP CODE = ${response.code}")
            Log.e("GroqApiService", "Analyse RAW RESPONSE = $responseBody")

            if (!response.isSuccessful) {
                throw Exception("Erreur API Groq : ${response.code} - $responseBody")
            }

            val root = gson.fromJson(responseBody, Map::class.java)

            val choices = root["choices"] as? List<*>
                ?: throw Exception("Réponse IA sans choices")

            if (choices.isEmpty()) {
                throw Exception("Réponse IA vide")
            }

            val message = (choices[0] as? Map<*, *>)?.get("message") as? Map<*, *>
                ?: throw Exception("Message IA vide")

            var content = message["content"] as? String
                ?: throw Exception("Contenu IA vide")

            Log.e("GroqApiService", "Analyse CONTENT = $content")

            content = cleanJson(content)

            Log.e("GroqApiService", "Analyse JSON CLEAN = $content")

            return@withContext gson.fromJson(content, AnalysisResponse::class.java)

        } catch (e: Exception) {
            Log.e("GroqApiService", "Erreur analyse IA", e)
            null
        }
    }

    private fun cleanJson(content: String): String {
        var json = content
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val start = json.indexOf("{")
        val end = json.lastIndexOf("}")

        if (start != -1 && end != -1 && end > start) {
            json = json.substring(start, end + 1)
        }

        return json
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
}