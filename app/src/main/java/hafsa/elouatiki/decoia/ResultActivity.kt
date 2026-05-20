package hafsa.elouatiki.decoia

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.gson.Gson
import hafsa.elouatiki.decoia.databinding.ActivityResultBinding
import java.io.File

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val resultJson = intent.getStringExtra("analysis_result")
        val generatedPath = intent.getStringExtra("generated_image_path")

        if (resultJson == null || generatedPath == null) {
            Toast.makeText(this, "Données non reçues", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            val analysis = gson.fromJson(resultJson, AnalysisResponse::class.java)
            displayResult(analysis, generatedPath)
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur d'affichage", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnNewProject.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }

    private fun displayResult(analysis: AnalysisResponse, imagePath: String) {
        val imgFile = File(imagePath)
        if (imgFile.exists()) {
            Glide.with(this)
                .load(imgFile)
                .into(binding.ivGeneratedImage)
        }

        binding.layoutPalette.removeAllViews()
        analysis.recommandations?.paletteCouleurs?.forEach { hex ->
            val view = View(this)
            val size = (40 * resources.displayMetrics.density).toInt()
            val params = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = (12 * resources.displayMetrics.density).toInt()
            }
            view.layoutParams = params
            
            val shape = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                try {
                    setColor(Color.parseColor(hex))
                } catch (e: Exception) {
                    setColor(Color.LTGRAY)
                }
            }
            view.background = shape
            binding.layoutPalette.addView(view)
        }

        val meubles = analysis.recommandations?.mobilier ?: emptyList()
        binding.rvProducts.layoutManager = LinearLayoutManager(this)
        binding.rvProducts.adapter = ProductAdapter(meubles)
    }
}
