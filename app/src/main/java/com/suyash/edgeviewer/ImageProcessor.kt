package com.suyash.edgeviewer

import org.opencv.core.Mat
import android.util.Log

class ImageProcessor {

    // External C++ function declarations
    external fun processFrame(matAddr: Long): Long
    external fun releaseMat(matAddr: Long)  // NEW: For memory cleanup

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

    /**
     * Process a frame and return the processed Mat.
     * IMPORTANT: Caller is responsible for releasing the returned Mat!
     */
    fun processFrameMat(inputMat: Mat): Mat {
        val outputAddr = processFrame(inputMat.nativeObjAddr)
        return Mat(outputAddr)
    }

    /**
     * Process and auto-release - safer but creates temporary Mat
     */
    fun processFrameSafe(inputMat: Mat, outputMat: Mat) {
        val processedAddr = processFrame(inputMat.nativeObjAddr)
        val tempMat = Mat(processedAddr)
        tempMat.copyTo(outputMat)
        releaseMat(processedAddr)  // Clean up C++ allocated memory
    }
}