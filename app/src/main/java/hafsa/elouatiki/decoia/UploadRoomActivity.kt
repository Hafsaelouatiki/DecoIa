package hafsa.elouatiki.decoia

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import hafsa.elouatiki.decoia.databinding.ActivityUploadRoomBinding
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class UploadRoomActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadRoomBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentImagePath: String? = null
    private var lastLocation: Location? = null

    // Gère la sélection dans la GALERIE
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.openInputStream(it)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    displaySelectedImage(bitmap)
                }
            } catch (e: Exception) {
                Log.e("UploadRoom", "Erreur galerie", e)
                Toast.makeText(this, "Impossible de charger l'image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Gère la prise de photo CAMÉRA (ActivityResultLauncher)
    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && currentImagePath != null) {
            val bitmap = BitmapFactory.decodeFile(currentImagePath)
            displaySelectedImage(bitmap)
        } else {
            Log.d("UploadRoom", "Prise de photo annulée ou échouée")
        }
    }

    // Gère les PERMISSIONS (Caméra et GPS)
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            fetchLocation()
        }
        if (perms[Manifest.permission.CAMERA] != true) {
            Toast.makeText(this, "La permission caméra est recommandée", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.tvWelcome.text = "Scanner IA : Bonjour ${FirebaseAuth.getInstance().currentUser?.displayName ?: "Utilisateur"}"

        // Clics sur les boutons
        binding.btnCapture.setOnClickListener { openCamera() }
        binding.btnGallery.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnStartAnalysis.setOnClickListener { runAIAnalysis() }

        // Demande des permissions au démarrage
        permissionLauncher.launch(arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))

        fetchLocation()
    }

    private fun openCamera() {
        try {
            val photoFile = File(cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
            currentImagePath = photoFile.absolutePath
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
            takePhotoLauncher.launch(uri)
        } catch (e: Exception) {
            Log.e("UploadRoom", "Erreur ouverture caméra", e)
            Toast.makeText(this, "Erreur lors de l'ouverture de la caméra", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displaySelectedImage(bitmap: Bitmap?) {
        if (bitmap == null) {
            Toast.makeText(this, "Erreur : image non récupérée", Toast.LENGTH_SHORT).show()
            return
        }
        binding.ivRoomPreview.setImageBitmap(bitmap)
        binding.ivRoomPreview.visibility = View.VISIBLE
        binding.btnStartAnalysis.visibility = View.VISIBLE
        
        // Masquer le viewFinder de CameraX s'il était présent
        binding.viewFinder.visibility = View.GONE
        binding.btnTakePicture.visibility = View.GONE
        
        // Sauvegarder l'image temporaire pour l'envoi
        val file = File(cacheDir, "upload_ready.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        currentImagePath = file.absolutePath
        
        Toast.makeText(this, "Image chargée avec succès", Toast.LENGTH_SHORT).show()
    }

    private fun fetchLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            binding.tvLocation.text = "Localisation : Permission manquante"
            return
        }
        
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    lastLocation = location
                    binding.tvLocation.text = "Localisation récupérée"
                    Log.d("GPS", "Position : ${location.latitude}, ${location.longitude}")
                } else {
                    binding.tvLocation.text = "Localisation non disponible"
                }
            }
        } catch (e: SecurityException) {
            Log.e("GPS", "Erreur accès GPS", e)
        }
    }

    private fun runAIAnalysis() {
        val path = currentImagePath
        if (path == null) {
            Toast.makeText(this, "Image manquante", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnStartAnalysis.isEnabled = false

        lifecycleScope.launch {
            val api = DecorApiService(this@UploadRoomActivity)
            
            // On tente l'analyse même si le GPS a échoué (valeurs par défaut 0.0)
            val resultPath = api.generateDecorImagePath(
                path, 
                "Modern elegant decoration", 
                lastLocation?.latitude ?: 0.0, 
                lastLocation?.longitude ?: 0.0
            )

            binding.progressBar.visibility = View.GONE
            binding.btnStartAnalysis.isEnabled = true

            if (resultPath != null) {
                val intent = Intent(this@UploadRoomActivity, ResultActivity::class.java)
                intent.putExtra("generated_image_path", resultPath)
                startActivity(intent)
            } else {
                Toast.makeText(this@UploadRoomActivity, "Erreur : Analyse échouée (Vérifie ton serveur Flask)", Toast.LENGTH_LONG).show()
            }
        }
    }
}