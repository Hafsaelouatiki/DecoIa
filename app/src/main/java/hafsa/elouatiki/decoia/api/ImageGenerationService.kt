package hafsa.elouatiki.decoia.api

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ImageGenerationService(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val accountId = "YOUR_API_KEY"
    private val apiToken = "YOUR_API_KEY"
    private val url = "https://api.cloudflare.com/client/v4/accounts/$accountId/ai/run/@cf/runwayml/stable-diffusion-v1-5-img2img"

    suspend fun generateAndSaveImage(prompt: String, originalBitmap: Bitmap? = null): String? = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("prompt", prompt)
                put("num_steps", 20)
                if (originalBitmap != null) {
                    val bytes = bitmapToByteArray(originalBitmap)
                    put("image", JSONArray().apply { bytes.forEach { put(it.toInt() and 0xFF) } })
                    put("strength", 0.6)
                }
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiToken")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 200) {
                    val bytes = response.body?.bytes()
                    if (bytes != null && bytes.size > 1000) {
                        val file = File(context.cacheDir, "gen_${System.currentTimeMillis()}.png")
                        FileOutputStream(file).use { it.write(bytes) }
                        return@withContext file.absolutePath
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ImageGen", "Erreur : ${e.message}")
        }
        null
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        val maxSide = 512
        val ratio = minOf(maxSide.toFloat() / bitmap.width, maxSide.toFloat() / bitmap.height)
        val scaled = if (ratio < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true) else bitmap
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return stream.toByteArray()
    }
}
