package hafsa.elouatiki.decoia.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import hafsa.elouatiki.decoia.R
import java.util.*

class MapsActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private lateinit var locationManager: LocationManager
    private lateinit var tvCityName: TextView
    private var locationOverlay: MyLocationNewOverlay? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osm", MODE_PRIVATE))
        setContentView(R.layout.activity_maps)

        map = findViewById(R.id.map)
        tvCityName = findViewById(R.id.tvCityName)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(17.0)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        checkPermissions()
    }

    private fun checkPermissions() {
        val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (perms.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, perms, 1)
        } else startLocationService()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationService() {
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map).apply {
            enableMyLocation()
            enableFollowLocation()
        }
        map.overlays.add(locationOverlay)

        val listener = object : LocationListener {
            override fun onLocationChanged(l: Location) { updateUI(l) }
            override fun onStatusChanged(p: String?, s: Int, b: Bundle?) {}
        }
        locationManager.getProviders(true).forEach {
            locationManager.requestLocationUpdates(it, 5000L, 10f, listener)
            locationManager.getLastKnownLocation(it)?.let { loc -> updateUI(loc) }
        }
    }

    private fun updateUI(location: Location) {
        val point = GeoPoint(location.latitude, location.longitude)
        map.controller.animateTo(point)
        updateCityName(location.latitude, location.longitude)

        map.overlays.removeAll { it is Marker && it.title == "Moi" }
        map.overlays.add(Marker(map).apply {
            position = point
            title = "Moi"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        })
        map.invalidate()
    }

    private fun updateCityName(lat: Double, lon: Double) {
        try {
            val addresses = Geocoder(this, Locale.getDefault()).getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) tvCityName.text = "📍 ${addresses[0].locality ?: "Ma Ville"}"
        } catch (e: Exception) {}
    }

    override fun onResume() { super.onResume(); map.onResume() }
    override fun onPause() { super.onPause(); map.onPause() }
}
