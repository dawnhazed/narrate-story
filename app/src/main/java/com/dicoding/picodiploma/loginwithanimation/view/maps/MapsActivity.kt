package com.dicoding.picodiploma.loginwithanimation.view.maps

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.dicoding.picodiploma.loginwithanimation.R
import com.dicoding.picodiploma.loginwithanimation.data.api.ListStoryItem

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.dicoding.picodiploma.loginwithanimation.databinding.ActivityMapsBinding
import com.dicoding.picodiploma.loginwithanimation.view.ViewModelFactory
import com.dicoding.picodiploma.loginwithanimation.view.main.StoryDetailViewModel
import com.dicoding.picodiploma.loginwithanimation.view.main.StoryDetailViewModelFactory
import com.google.android.gms.maps.model.LatLngBounds

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    private val viewModel by viewModels<MapsViewModel> {
        ViewModelFactory.getInstance(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        viewModel.stories.observe(this, Observer{ stories ->
            if (stories != null) {
                Log.d("MapsActivity", "Stories received: ${stories.size}")
                addMarkers(stories)
            } else {
                Log.d("MapsActivity", "No stories found")
            }
        })

        viewModel.fetchStoriesWithLocation()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isIndoorLevelPickerEnabled = true
        mMap.uiSettings.isCompassEnabled = true
        mMap.uiSettings.isMapToolbarEnabled = true

        addMarkerFromUpload()
    }

    private fun addMarkers(stories: List<ListStoryItem>) {

        if (stories.isEmpty()) {
            Log.d("MapsActivity", "No stories to add markers")
            return
        }

        val boundsBuilder = LatLngBounds.Builder()

        stories.forEach { story ->
            val position = LatLng(story.lat, story.lon)
            Log.d("MapsActivity", "Adding marker at: $position")
            mMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(story.name)
                    .snippet(story.description)
            )
            boundsBuilder.include(position)
        }

        // Adjust the camera to include all markers
        val bounds = boundsBuilder.build()
        val padding = 100 // offset from edges of the map in pixels
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        mMap.moveCamera(cameraUpdate)

        /*
        for (story in stories) {
            val position = LatLng(story.lat, story.lon)
            mMap.addMarker(MarkerOptions().position(position).title(story.name))
        }
        // Optionally, move the camera to the first story location
        if (stories.isNotEmpty()) {
            val firstStory = stories[0]
            val firstPosition = LatLng(firstStory.lat, firstStory.lon)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(firstPosition, 10f))
        } */
    }

    private fun addMarkerFromUpload() {
        val sharedPreferences = getSharedPreferences("prefs", MODE_PRIVATE)
        val lat = sharedPreferences.getFloat("LATITUDE", 0f)
        val lon = sharedPreferences.getFloat("LONGITUDE", 0f)

        if (lat != 0f && lon != 0f) {
            val position = LatLng(lat.toDouble(), lon.toDouble())
            Log.d("MapsActivity", "Adding marker from upload at: $position")
            mMap.addMarker(MarkerOptions().position(position).title("Last Added Location"))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
        }
    }

    private fun getToken(): String {
        val sharedPreferences = getSharedPreferences("prefs", MODE_PRIVATE)
        return sharedPreferences.getString("token", "") ?: ""
    }
}
