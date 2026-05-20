package hafsa.elouatiki.decoia

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object NetworkClient {
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
}