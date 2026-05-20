package hafsa.elouatiki.decoia

import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ShoppingService {
    suspend fun searchProduct(query: String): ShoppingResult? = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://www.googleapis.com/customsearch/v1?key=${ApiKeys.GOOGLE_SEARCH_KEY}&cx=${ApiKeys.GOOGLE_CX}&q=$encodedQuery")
            
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (connection.responseCode != 200) return@withContext null

            val responseText = connection.inputStream.bufferedReader().readText()
            val items = JsonParser.parseString(responseText).asJsonObject.getAsJsonArray("items")
            
            if (items == null || items.size() == 0) return@withContext null

            val firstItem = items[0].asJsonObject
            val title = firstItem.get("title")?.asString ?: ""
            val link = firstItem.get("link")?.asString ?: ""
            val siteName = firstItem.get("displayLink")?.asString ?: ""
            var imageUrl = ""

            try {
                val pagemap = firstItem.getAsJsonObject("pagemap")
                val cseImage = pagemap?.getAsJsonArray("cse_image")
                if (cseImage != null && cseImage.size() > 0) {
                    imageUrl = cseImage[0].asJsonObject.get("src")?.asString ?: ""
                }
            } catch (e: Exception) {}

            ShoppingResult(title, link, imageUrl, siteName, "Prix sur site")
        } catch (e: Exception) {
            Log.e("Shopping", "Erreur : ${e.message}")
            null
        }
    }
}
