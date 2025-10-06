package com.suyash.edgeviewer

import org.opencv.core.Mat
import android.util.Log

class ImageProcessor {

    // External C++ function declaration
    external fun processFrame(matAddr: Long): Long

    companion object {
        private const val TAG = "ImageProcessor"

        // Load native libraries
        init {
            try {
                System.loadLibrary("opencv_java4")  // OpenCV library
                System.loadLibrary("edgeviewer")     // Your native library
                Log.d(TAG, "Native libraries loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Error loading native libraries: ${e.message}")
            }
        }
    }

    // Convenience function that handles Mat objects directly
    fun processFrameMat(inputMat: Mat): Mat {
        val outputAddr = processFrame(inputMat.nativeObjAddr)
        return Mat(outputAddr)
    }
}