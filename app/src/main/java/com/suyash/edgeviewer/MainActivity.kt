package com.suyash.edgeviewer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private lateinit var cameraBridge: CameraBridgeViewBase
    private lateinit var processor: ImageProcessor
    private lateinit var btnToggleMode: Button
    private lateinit var tvFps: TextView

    private var isEdgeDetectionEnabled = true
    private var frameCount = 0
    private var lastTime = System.currentTimeMillis()
    private var currentFps = 0.0

    // Reusable Mat objects to avoid memory allocation in onCameraFrame
    private var rgbaMat: Mat? = null
    private var processedMat: Mat? = null

    companion object {
        private const val TAG = "MainActivity"
        private const val CAMERA_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV initialization failed!")
            Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG).show()
            return
        } else {
            Log.d(TAG, "OpenCV loaded successfully")
        }

        // Initialize processor
        processor = ImageProcessor()

        // Initialize UI elements
        cameraBridge = findViewById(R.id.camera_view)
        btnToggleMode = findViewById(R.id.btn_toggle_mode)
        tvFps = findViewById(R.id.tv_fps)

        // Set up camera
        cameraBridge.visibility = CameraBridgeViewBase.VISIBLE
        cameraBridge.setCvCameraViewListener(this)

        // Toggle button
        btnToggleMode.setOnClickListener {
            isEdgeDetectionEnabled = !isEdgeDetectionEnabled
            btnToggleMode.text = if (isEdgeDetectionEnabled) {
                "Edge Detection ON"
            } else {
                "Raw Feed"
            }
        }

        // Check and request camera permission
        checkCameraPermission()
    }

    private fun checkCameraPermission() {if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            Log.d(TAG, "Permission is already granted.")
            // Inform the view that permission is granted
            cameraBridge.setCameraPermissionGranted()
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Inform the view that permission is granted
                cameraBridge.setCameraPermissionGranted()
                cameraBridge.enableView()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }


    override fun onCameraViewStarted(width: Int, height: Int) {
        Log.d(TAG, "Camera started: ${width}x${height}")

        if (width == 0 || height == 0) {
            Log.e(TAG, "Invalid camera dimensions!")
            runOnUiThread {
                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_LONG).show()
            }
            return
        }

        // Pre-allocate Mat objects
        rgbaMat = Mat()
        processedMat = Mat()
    }

    override fun onCameraViewStopped() {
        Log.d(TAG, "Camera stopped")
        // Release Mat objects
        rgbaMat?.release()
        processedMat?.release()
    }



    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        // Calculate FPS
        frameCount++
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTime >= 1000) {
            currentFps = (frameCount * 1000.0) / (currentTime - lastTime)
            frameCount = 0
            lastTime = currentTime
            runOnUiThread {
                tvFps.text = String.format("FPS: %.1f", currentFps)
            }
        }

        val rgba = inputFrame.rgba()
        if (rgba.empty()) {
            Log.e(TAG, "Received empty frame, returning it.")
            return rgba
        }

        if (!isEdgeDetectionEnabled) {
            return rgba // Return raw feed if detection is OFF
        }

        // --- Processing Logic ---
        val outputMat = processedMat
        if (outputMat == null) {
            Log.e(TAG, "processedMat is null, returning raw frame as fallback.")
            return rgba
        }

        try {
            // Process the frame and get the address of the C++ Mat
            val processedAddr = processor.processFrame(rgba.nativeObjAddr)

            // Create a temporary Java wrapper for the C++ Mat
            val tempMat = Mat(processedAddr)

            // Copy the processed data from the temp Mat to our stable output Mat
            tempMat.copyTo(outputMat)

            // IMPORTANT: Release the C++ memory immediately after copying
            processor.releaseMat(processedAddr)

            // Return the stable Mat that now holds the processed frame
            return outputMat

        } catch (e: Exception) {
            Log.e(TAG, "Critical error in native processing: ${e.message}", e)
            // If native processing fails, safely return the original raw frame to avoid a crash
            return rgba
        }
    }


    override fun onResume() {
        super.onResume()
        // Check for permission again in case the user revoked it while the app was paused
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {Log.d(TAG, "Resuming camera view.")
            cameraBridge.enableView()
        } else {
            Log.e(TAG, "Camera permission not granted on resume.")
            // You might want to re-request permission or show a message
            checkCameraPermission()
        }
    }


    override fun onPause() {
        super.onPause()
        cameraBridge.disableView()
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraBridge.disableView()
        // Clean up Mat objects
        rgbaMat?.release()
        processedMat?.release()
    }
}