package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import okhttp3.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentKey: String? = null


    // UI elements
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var keyEditText: EditText

    private var isTracking = false
    private var locationRequest: LocationRequest? = null

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
        // In a real app, you would validate the key against your database.
        private const val VALID_KEY = "SAMPLE_KEY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make sure your layout contains startButton, stopButton, and keyEditText with the correct IDs.
        setContentView(R.layout.activity_main)

        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        keyEditText = findViewById(R.id.keyEditText)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    Log.d("MainActivity", "Location: ${location.latitude}, ${location.longitude}")
                    sendLocationToPhpServer(location.latitude, location.longitude)
                }
            }
        }

        startButton.setOnClickListener {
            val enteredKey = keyEditText.text.toString().trim()
            if (enteredKey.isEmpty()) {
                Toast.makeText(this, "Please enter a key", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            validateKeyFromServer(enteredKey)
        }


        stopButton.setOnClickListener {
            stopLocationUpdates()
        }
    }

    private fun validateKeyFromServer(key: String) {
        val url = "https://cold-breads-hug.loca.lt/validatekey.php?key=$key"
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Validation failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val result = response.body?.string()?.trim()
                runOnUiThread {
                    if (result == "valid") {
                        currentKey = keyEditText.text.toString().trim() // ✅ Save the key after validation
                        startLocationUpdates()
                    } else {
                        Toast.makeText(this@MainActivity, "Invalid key. Tracking not started.", Toast.LENGTH_SHORT).show()
                    }
                }
            }



        })
    }

    private fun startLocationUpdates() {
        if (isTracking) return

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 60000L // 1-minute interval
        ).setMinUpdateIntervalMillis(30000L) // fastest 30 seconds
            .build()

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_REQUEST_CODE
            )
            return
        }

        fusedLocationClient.requestLocationUpdates(locationRequest!!, locationCallback, Looper.getMainLooper())
        Toast.makeText(this, "Tracking started", Toast.LENGTH_SHORT).show()
        isTracking = true
    }

    private fun stopLocationUpdates() {
        if (!isTracking) return
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Toast.makeText(this, "Tracking stopped", Toast.LENGTH_SHORT).show()
        isTracking = false
    }

    private fun sendLocationToPhpServer(latitude: Double, longitude: Double) {
        if (currentKey.isNullOrEmpty()) {
            Log.w("MainActivity", "Key not set, not sending location")
            return
        }
// type your own local tunnel URL below!!
        val url = "https://cold-breads-hug.loca.lt/locationtrack.php?x=$latitude&y=$longitude&key=${currentKey}"
        Log.d("MainActivity", "Sending location: $url")

        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MainActivity", "Failed to send location: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.i("MainActivity", "Location sent: ${response.body?.string()}")
            }
        })
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }
}
