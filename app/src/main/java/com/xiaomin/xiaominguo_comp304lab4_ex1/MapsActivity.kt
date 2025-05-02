package com.xiaomin.xiaominguo_comp304lab4_ex1

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.xiaomin.xiaominguo_comp304lab4_ex1.databinding.ActivityMapsBinding
import com.google.android.gms.maps.model.PolylineOptions
import android.graphics.Color
import android.os.Looper
import android.util.Log
import android.widget.Button
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private val mapViewBundleKey = "MapViewBundleKey"

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private lateinit var geofencingClient: GeofencingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize fusedLocationClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Initialize geofencingClient
        geofencingClient = LocationServices.getGeofencingClient(this)

        // Set up location request for real-time location updates using the builder pattern
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)  // Fastest update interval in milliseconds (5 seconds)
            .build()

        // Create a location callback to handle location updates
        locationCallback= object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach{ location ->
                    // Update latitude and longitude when location changes
                    updateCurrentLocation(location.latitude, location.longitude)
                }
            }
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize the back button and set its click listener
        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // Close the MapsActivity and go back to the previous activity
        }

        // Retrieve the saved instance state
        val mapViewBundle = savedInstanceState?.getBundle(mapViewBundleKey)
        // Restore the map state if available
        mapViewBundle?.let {
            val cameraPosition = it.getParcelable<CameraPosition>("camera_position")
            cameraPosition?.let { position ->
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(position))
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Retrieve location data from Intent
        val latitude = intent.getDoubleExtra("LATITUDE", 0.0)
        val longitude = intent.getDoubleExtra("LONGITUDE", 0.0)
        val name = intent.getStringExtra("NAME") ?: "Destination Location"
        val currentLatitude = intent.getDoubleExtra("CURRENT_LAT", 0.0)
        val currentLongitude = intent.getDoubleExtra("CURRENT_LNG", 0.0)
        val currentName = "Your Location"

        // Create LatLng for the clicked location and current location
        val clickedLocation = LatLng(latitude, longitude)
        val currentLocation = LatLng(currentLatitude, currentLongitude)

        // Add a marker for the clicked location (i.e. destination)
        mMap.addMarker(MarkerOptions().position(clickedLocation).title(name))
        // Add a marker for the current location
        mMap.addMarker(MarkerOptions().position(currentLocation).title(currentName))

        // geofence of 500 meters radius around destination location
        createGeofence(clickedLocation, 500f, "destination_geofence")

        // Draw polyline between current location and clicked location
        mMap.addPolyline(
            PolylineOptions()
                .add(currentLocation, clickedLocation)
                .width(8f)
                .color(Color.BLUE)
        )

        // Move camera to the clicked location, zoom in to show both locations
        val boundsBuilder = LatLngBounds.Builder()
        boundsBuilder.include(clickedLocation)
        boundsBuilder.include(currentLocation)

        val bounds = boundsBuilder.build()
        val padding = 100  // Optional, to add padding around the markers
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        mMap.moveCamera(cameraUpdate)

        // Add click listener
        mMap.setOnMapClickListener { latLng ->
            Toast.makeText(this, "${latLng.latitude}, ${latLng.longitude}", Toast.LENGTH_SHORT).show()
        }

        // Add long click listener
        mMap.setOnMapLongClickListener { latLng ->
            Snackbar.make(binding.root, "${latLng.latitude}, ${latLng.longitude}", Snackbar.LENGTH_LONG).show()
        }

        //tracking real time update of user current location
        startLocationUpdates();
    }

    //Save the map’s current state
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val mapViewBundle = outState.getBundle(mapViewBundleKey) ?: Bundle()

        // Save the map state (camera position includes the zoom level, and it also includes other elements such as tilt and bearing.no)
        mMap.let {
            val cameraPosition = it.cameraPosition
            mapViewBundle.putParcelable("camera_position", cameraPosition)
        }
        outState.putBundle(mapViewBundleKey, mapViewBundle)
    }

    // Function to update current location dynamically
    fun updateCurrentLocation(lat: Double, lng: Double) {
        val newLatLng = LatLng(lat, lng)

        mMap.clear() // Clears all markers, polyline, etc., to avoid overlap
        mMap.addMarker(MarkerOptions().position(newLatLng).title("Your Location"))

        // Re-add the destination marker (you can keep it from the original intent)
        val latitude = intent.getDoubleExtra("LATITUDE", 0.0)
        val longitude = intent.getDoubleExtra("LONGITUDE", 0.0)
        val clickedLocation = LatLng(latitude, longitude)
        val name = intent.getStringExtra("NAME") ?: "Destination Location"
        mMap.addMarker(MarkerOptions().position(clickedLocation).title(name))

        // Re-draw polyline between the new current location and the clicked location
        mMap.addPolyline(
            PolylineOptions()
                .add(newLatLng, clickedLocation)
                .width(8f)
                .color(Color.BLUE)
        )

        // Move camera to the new current location
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(newLatLng))
        // Create LatLngBounds to include both current and destination locations
        val boundsBuilder = LatLngBounds.Builder()
        boundsBuilder.include(newLatLng)
        boundsBuilder.include(clickedLocation)

        val bounds = boundsBuilder.build()
        val padding = 100 // Optional, to add padding around the markers

        // Move camera to fit both locations in the screen
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        mMap.moveCamera(cameraUpdate)
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } else {
            // If not granted, show an error about permission
            Toast.makeText(this, "Location permission is required to track location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onPause() {
        super.onPause()
        // Stop location updates when activity is paused
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        // Start location updates when activity is resumed
        startLocationUpdates()
    }

    private fun createGeofence(latLng: LatLng, radius: Float, id: String) {
        val geofence = Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(
                latLng.latitude,
                latLng.longitude,
                radius
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val geofencePendingIntent: PendingIntent by lazy {
            val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
            PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Location permission needed for geofencing", Toast.LENGTH_SHORT).show()
            return
        }

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
            .addOnSuccessListener {
                Log.d("Geofence", "Geofence added")
            }
            .addOnFailureListener {
                Log.e("Geofence", "Failed to add geofence: ${it.message}")
            }
    }
}