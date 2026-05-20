package hafsa.elouatiki.decoia

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class DecorApiService(private val context: Context) {

    private val TAG = "DecorApiService"
    // Adresse pour l'émulateur Android vers ton PC
    private val baseUrl = "http://10.0.2.2:5000"

    suspend fun generateDecorImagePath(
        imagePath: String,
        prompt: String,
        lat: Double,
        lon: Double
    ): String? = withContext(Dispatchers.IO) {
        try {
            val imageFile = File(imagePath)
            if (!imageFile.exists()) {
                Log.e(TAG, "Image introuvable à l'adresse : $imagePath")
                return@withContext null
            }

            // Préparation de l'envoi Multipart (Image + Prompt + GPS)
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", imageFile.name, imageFile.asRequestBody("image/jpeg".toMediaType()))
                .addFormDataPart("prompt", prompt)
                .addFormDataPart("latitude", lat.toString())
                .addFormDataPart("longitude", lon.toString())
                .build()

            val request = Request.Builder()
                .url("$baseUrl/generate-decor")
                .post(requestBody)
                .build()

            Log.d(TAG, "Tentative de connexion au backend : $baseUrl/generate-decor")

            NetworkClient.okHttpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                Log.d(TAG, "Code réponse : ${response.code}")

                if (!response.isSuccessful || responseBody == null) {
                    Log.e(TAG, "Erreur serveur : ${response.code} - $responseBody")
                    return@withContext null
                }

                val json = JSONObject(responseBody)
                if (json.optBoolean("success", false)) {
                    // Si l'IA a réussi, on décode l'image reçue en Base64
                    val base64Image = json.getString("image_base64")
                    val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                    
                    val outputFile = File(context.cacheDir, "result_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(outputFile).use { it.write(imageBytes) }
                    
                    return@withContext outputFile.absolutePath
                } else {
                    Log.e(TAG, "Le backend a retourné une erreur : ${json.optString("error")}")
                    return@withContext null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connexion impossible ! Vérifie que ton serveur Flask tourne sur http://10.0.2.2:5000", e)
            return@withContext null
        }
    }
}
