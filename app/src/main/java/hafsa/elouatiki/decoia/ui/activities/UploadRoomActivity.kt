package hafsa.elouatiki.decoia.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import hafsa.elouatiki.decoia.LoadingActivity
import hafsa.elouatiki.decoia.databinding.ActivityUploadRoomBinding
import java.io.File
import java.io.FileOutputStream

class UploadRoomActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUploadRoomBinding
    private var selectedBitmap: Bitmap? = null
    private var imageCapture: ImageCapture? = null

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (it[Manifest.permission.CAMERA] == true) startCamera()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCapture.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera()
            else permLauncher.launch(arrayOf(Manifest.permission.CAMERA))
        }

        binding.btnTakePicture.setOnClickListener { takePhoto() }
        binding.btnGallery.setOnClickListener { galleryLauncher.launch("image/*") }
        binding.btnStartAnalysis.setOnClickListener { startAnalysis() }
        
        val user = FirebaseAuth.getInstance().currentUser
        binding.tvWelcome.text = "Scanner IA : Bonjour ${user?.displayName ?: "Utilisateur"}"
    }

    private fun startCamera() {
        ProcessCameraProvider.getInstance(this).addListener({
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }
            imageCapture = ImageCapture.Builder().build()
            try {
                val provider = ProcessCameraProvider.getInstance(this).get()
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                binding.viewFinder.visibility = View.VISIBLE
                binding.btnTakePicture.visibility = View.VISIBLE
            } catch (e: Exception) {}
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val file = File(cacheDir, "cap.jpg")
        imageCapture?.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(res: ImageCapture.OutputFileResults) {
                    updatePreview(BitmapFactory.decodeFile(file.absolutePath))
                }
                override fun onError(e: ImageCaptureException) {}
            })
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { 
            contentResolver.openInputStream(it)?.use { stream ->
                updatePreview(BitmapFactory.decodeStream(stream))
            }
        }
    }

    private fun updatePreview(bmp: Bitmap?) {
        bmp?.let {
            selectedBitmap = it
            binding.ivRoomPreview.setImageBitmap(it)
            binding.viewFinder.visibility = View.GONE
            binding.btnStartAnalysis.visibility = View.VISIBLE
        }
    }

    private fun startAnalysis() {
        selectedBitmap?.let { bmp ->
            val file = File(cacheDir, "temp.jpg")
            FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            val path = file.absolutePath

            val intent = Intent(this@UploadRoomActivity, LoadingActivity::class.java)
            intent.putExtra("image_path", path)
            startActivity(intent)
        }
    }
}
