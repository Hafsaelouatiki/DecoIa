package hafsa.elouatiki.decoia

import com.google.gson.annotations.SerializedName

data class AnalysisResponse(
    @SerializedName("style_actuel") val styleActuel: String? = "",
    @SerializedName("couleurs_actuelles") val couleursActuelles: List<String>? = emptyList(),
    @SerializedName("type_piece") val typePiece: String? = "",
    @SerializedName("recommandations") val recommandations: Recommandations? = null
)

data class Recommandations(
    @SerializedName("style_cible") val styleCible: String? = "",
    @SerializedName("palette_couleurs") val paletteCouleurs: List<String>? = emptyList(),
    @SerializedName("mobilier") val mobilier: List<Mobilier>? = emptyList(),
    @SerializedName("prompt_image") val promptImage: String? = ""
)

data class Mobilier(
    val nom: String? = "",
    val recherche: String? = "",
    var shoppingResult: ShoppingResult? = null
)

data class ShoppingResult(
    val title: String? = "",
    val link: String? = "",
    val imageUrl: String? = "",
    val siteName: String? = "",
    val price: String? = "Prix sur site"
)

data class ChatMessage(val message: String, val isUser: Boolean, val timestamp: Long = System.currentTimeMillis())
