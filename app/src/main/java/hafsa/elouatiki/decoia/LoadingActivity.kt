package hafsa.elouatiki.decoia

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.launch

class LoadingActivity : AppCompatActivity() {

    private val groqApiService = GroqApiService()
    private val shoppingService = ShoppingService()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        val imagePath = intent.getStringExtra("image_path")

        if (imagePath.isNullOrEmpty()) {
            Log.e("LoadingActivity", "image_path est vide ou null")
            Toast.makeText(this, "Erreur : image introuvable", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        Log.d("LoadingActivity", "Image path reçu = $imagePath")

        val bitmap = BitmapFactory.decodeFile(imagePath)

        if (bitmap == null) {
            Log.e("LoadingActivity", "Impossible de décoder l'image : $imagePath")
            Toast.makeText(this, "Erreur : image invalide", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val imageGen = ImageGenerationService(this)

        lifecycleScope.launch {
            try {
                Log.d("LoadingActivity", "Début analyse IA")

                val analysis = groqApiService.analyzeRoom(bitmap)

                if (analysis == null) {
                    Log.e("LoadingActivity", "GroqApiService a retourné null")
                    throw Exception("Analyse IA vide")
                }

                Log.d("LoadingActivity", "Analyse IA réussie")

                val prompt = analysis.recommandations?.promptImage
                    ?.takeIf { it.isNotBlank() }
                    ?: "Modern interior decoration, elegant furniture, clean room, realistic style"

                Log.e("LoadingActivity", "Prompt envoyé à ImageGenerationService = $prompt")

                Log.d("LoadingActivity", "Début génération image")

                val genPath = imageGen.generateAndSaveImage(prompt, bitmap)

                if (genPath == null) {
                    Log.e("LoadingActivity", "ImageGenerationService a retourné null")
                    throw Exception("Génération image échouée")
                }

                analysis.recommandations?.mobilier?.forEach {
                    it.shoppingResult = shoppingService.searchProduct(it.recherche ?: "")
                }

                startActivity(Intent(this@LoadingActivity, ResultActivity::class.java).apply {
                    putExtra("analysis_result", gson.toJson(analysis))
                    putExtra("generated_image_path", genPath)
                })

                finish()

            } catch (e: Exception) {
                Log.e("LoadingActivity", "Erreur complète dans LoadingActivity", e)

                Toast.makeText(
                    this@LoadingActivity,
                    "Erreur : ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()

                finish()
            }
        }
    }
}