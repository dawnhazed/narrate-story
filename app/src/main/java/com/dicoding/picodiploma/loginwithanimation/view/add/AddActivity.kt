package com.dicoding.picodiploma.loginwithanimation.view.add

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MultipartBody
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.dicoding.picodiploma.loginwithanimation.R
import com.dicoding.picodiploma.loginwithanimation.data.api.ApiConfig
import com.dicoding.picodiploma.loginwithanimation.data.api.ApiService
import com.dicoding.picodiploma.loginwithanimation.data.api.FileUploadResponse
import com.dicoding.picodiploma.loginwithanimation.databinding.ActivityAddBinding
import com.dicoding.picodiploma.loginwithanimation.view.main.MainActivity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class AddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var currentImageUri: Uri? = null
    private var currentLat: Double? = null
    private var currentLon: Double? = null

    private val apiService: ApiService by lazy {
        val token = getToken()
        ApiConfig.getApiService(token)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Permission request granted", Toast.LENGTH_LONG).show()
                checkLocationSettings()
            } else {
                Toast.makeText(this, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            this,
            REQUIRED_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    LOCATION_PERMISSION
                ) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()

        showLoading(false)

        if (!allPermissionsGranted()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSION)
            requestPermissionLauncher.launch(LOCATION_PERMISSION)
        } else {
            checkLocationSettings()
        }

        binding.btnGallery.setOnClickListener { startGallery() }
        binding.btnCamera.setOnClickListener { startCamera() }
        binding.btnUpload.setOnClickListener { startUpload() }
    }

    private fun setupLocationCallback() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location: Location? = locationResult.lastLocation
                if (location != null) {
                    currentLat = location.latitude
                    currentLon = location.longitude
                    Log.d("Location", "Lat: $currentLat, Lon: $currentLon")
                    fusedLocationClient.removeLocationUpdates(this)
                } else {
                    Log.d("Location", "Location is null")
                    Toast.makeText(this@AddActivity, "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkLocationSettings() {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            getCurrentLocation()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this@AddActivity, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    private fun startGallery() {
        Toast.makeText(this, "Gallery Button Clicked!", Toast.LENGTH_SHORT).show()
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if ( uri != null ) {
            currentImageUri = uri
            showImage()
        } else {
            Log.d("Gallery", "No Media Selected")
        }
    }

    private fun showImage() {
        currentImageUri?.let {
            Log.d("Image URI", "showImage: $it")
            binding.ivPreview.setImageURI(it)
        }
    }

    private fun startCamera() {
        val uri = getImageUri(this)
        currentImageUri = uri
        launcherIntentCamera.launch(uri)
    }

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            showImage()
        }
    }

    private fun startUpload() {
        val description = binding.etInput.text.toString()

        if (description.isEmpty()) {
            showToast("Deskripsi tidak boleh kosong.")
            return
        }

        if (currentImageUri == null) {
            showToast("Gambar tidak boleh kosong.")
            return
        }

        currentImageUri?.let { uri ->
            val imageFile = uriToFile(uri, this)
            val compressedImageFile = compressImageFile(imageFile)
            if (compressedImageFile.length() > 1_000_000) {
                showToast("Gambar masih terlalu besar setelah kompresi. Silakan pilih gambar lain.")
                return
            }

            showLoading(true)

            val requestBody = description.toRequestBody("text/plain".toMediaType())
            val requestImageFile = compressedImageFile.asRequestBody("image/jpeg".toMediaType())
            val multipartBody = MultipartBody.Part.createFormData(
                "photo",
                compressedImageFile.name,
                requestImageFile
            )

            val token = getToken()
            Log.d("token", "token : $token")

            val lat = currentLat?.toFloat()
            val lon = currentLon?.toFloat()

            if (lat != null && lon != null) {
                Log.d("Upload", "Lat: $lat, Lon: $lon")
                // Save lat and lon in SharedPreferences
                val sharedPreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE)
                sharedPreferences.edit().apply {
                    putFloat("LATITUDE", lat)
                    putFloat("LONGITUDE", lon)
                    apply()
                }
            } else {
                Log.d("Upload", "Lat or Lon is null")
            }

            lifecycleScope.launch {
                try {
                    val apiService = ApiConfig.getApiService(token)
                    val successResponse = apiService.uploadImage(multipartBody, requestBody, lat, lon)
                    showToast(successResponse.message)
                    Log.d("upload story", "Upload Success")
                    Log.d("upload story", "lat = $lat & lon = $lon")

                    val intent = Intent(this@AddActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()

                    showLoading(false)

                } catch (e: HttpException) {
                    val errorBody = e.response()?.errorBody()?.string()
                    val errorResponse = Gson().fromJson(errorBody, FileUploadResponse::class.java)
                    showToast(errorResponse.message)
                    Log.d("upload story", "Upload Failed")
                    showLoading(false)
                }
            }

        } ?: showToast("No Image Found")


    }

    private fun compressImageFile(imageFile: File): File {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(imageFile.path, this)
            inSampleSize = calculateInSampleSize(this, 1024, 1024)
            inJustDecodeBounds = false
        }

        val bitmap = BitmapFactory.decodeFile(imageFile.path, options)
        val byteArrayOutputStream = ByteArrayOutputStream()

        var quality = 80
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)

        while (byteArrayOutputStream.toByteArray().size > 1_000_000 && quality > 10) {
            byteArrayOutputStream.reset()
            quality -= 5
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        }

        val compressedImageFile = File(imageFile.parent, "compressed_" + imageFile.name)
        FileOutputStream(compressedImageFile).use {
            it.write(byteArrayOutputStream.toByteArray())
        }

        return compressedImageFile
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun getCurrentLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLat = location.latitude
                    currentLon = location.longitude
                    Log.d("Location", "Lat: $currentLat, Lon: $currentLon")
                } else {
                    Log.d("Location", "Last location is null, requesting location updates")
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
                }
            }.addOnFailureListener {
                Log.d("Location", "Failed to get last location, requesting location updates")
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun getToken(): String {
        val sharedPreferences = getSharedPreferences("prefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("token", "") ?: ""
        Log.d("Token Retrieved", "Token: $token")
        return token
    }

    companion object {
        private const val REQUIRED_PERMISSION = Manifest.permission.CAMERA
        private const val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
        private const val REQUEST_CHECK_SETTINGS = 1001
    }

}