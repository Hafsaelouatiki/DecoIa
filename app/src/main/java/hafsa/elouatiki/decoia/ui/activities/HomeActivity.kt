package hafsa.elouatiki.decoia.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import hafsa.elouatiki.decoia.LoginActivity
import hafsa.elouatiki.decoia.R
import hafsa.elouatiki.decoia.databinding.ActivityHomeBinding
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var locationManager: LocationManager
    private val mAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupBottomNav()
        fetchUserName()
        checkLocationPermission()
    }

    private fun setupUI() {
        binding.btnHeaderLogout.setOnClickListener { showLogoutDialog() }
        binding.btnAnalyzeIA.setOnClickListener { 
            startActivity(Intent(this@HomeActivity, UploadRoomActivity::class.java)) 
        }
        binding.featPhoto.tvFeatureName.text = "Scanner IA"
        binding.featPhoto.root.setOnClickListener { 
            startActivity(Intent(this@HomeActivity, UploadRoomActivity::class.java)) 
        }
        binding.featChat.tvFeatureName.text = "Chat Expert"
        binding.featChat.root.setOnClickListener { 
            startActivity(Intent(this@HomeActivity, ChatActivity::class.java)) 
        }
        binding.btnOpenMap.setOnClickListener { 
            startActivity(Intent(this@HomeActivity, MapsActivity::class.java)) 
        }
    }

    private fun fetchUserName() {
        val user = mAuth.currentUser ?: return
        db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
            binding.tvWelcome.text = "Bonjour ${doc.getString("name") ?: "Utilisateur"} 👋"
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_map -> { 
                    startActivity(Intent(this@HomeActivity, MapsActivity::class.java))
                    true 
                }
                R.id.nav_chat -> { 
                    startActivity(Intent(this@HomeActivity, ChatActivity::class.java))
                    true 
                }
                R.id.nav_profile -> { 
                    showLogoutDialog()
                    true 
                }
                else -> false
            }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this@HomeActivity)
            .setTitle("Déconnexion")
            .setMessage("Voulez-vous quitter DecoIA ?")
            .setPositiveButton("Oui") { _: DialogInterface, _: Int ->
                mAuth.signOut()
                val intent = Intent(this@HomeActivity, LoginActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        } else {
            startLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)
        val listener = object : LocationListener {
            override fun onLocationChanged(l: Location) { updateLocationUI(l) }
            override fun onStatusChanged(p0: String?, s: Int, b: Bundle?) {}
        }
        providers.forEach { 
            locationManager.requestLocationUpdates(it, 10000L, 10f, listener) 
        }
    }

    private fun updateLocationUI(location: Location) {
        try {
            val addresses = Geocoder(this, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                binding.tvLocation.text = "📍 ${addresses[0].locality ?: "Ma Ville"}"
            }
        } catch (e: Exception) {}
    }
}
