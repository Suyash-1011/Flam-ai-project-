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

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            cameraBridge.enableView()
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
                cameraBridge.enableView()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        Log.d(TAG, "Camera started: ${width}x${height}")
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
        // Get the input frame
        val rgba = inputFrame.rgba()

        // Calculate FPS
        frameCount++
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastTime

        if (elapsed >= 1000) { // Update FPS every second
            currentFps = (frameCount * 1000.0) / elapsed
            frameCount = 0
            lastTime = currentTime

            runOnUiThread {
                tvFps.text = String.format("FPS: %.1f", currentFps)
            }
        }

        // Return processed or raw frame
        return if (isEdgeDetectionEnabled) {
            try {
                // Process frame with memory management
                val processedAddr = processor.processFrame(rgba.nativeObjAddr)
                val tempMat = Mat(processedAddr)

                // Copy to reusable Mat
                processedMat?.let { output ->
                    tempMat.copyTo(output)
                    // Clean up C++ allocated memory
                    processor.releaseMat(processedAddr)
                    output
                } ?: tempMat

            } catch (e: Exception) {
                Log.e(TAG, "Error processing frame: ${e.message}")
                rgba // Return original on error
            }
        } else {
            rgba // Return raw camera feed
        }
    }

    override fun onResume() {
        super.onResume()
        if (OpenCVLoader.initDebug()) {
            cameraBridge.enableView()
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