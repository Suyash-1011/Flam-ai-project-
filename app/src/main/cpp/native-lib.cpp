#include <jni.h>
#include <string>

// OpenCV core and image processing headers
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp> // <-- ADD THIS for cvtColor and Canny

// Android logging header
#include "android/log.h"

#define TAG "NativeLib"
// Corrected the typo from ANDROIF to ANDROID
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

// Forward declaration for the release function
extern "C" JNIEXPORT void JNICALL
Java_com_suyash_edgeviewer_ImageProcessor_releaseMat(JNIEnv* env, jobject /* this */, jlong matAddr);

extern "C" JNIEXPORT jlong JNICALL
Java_com_suyash_edgeviewer_ImageProcessor_processFrame(
        JNIEnv* env,
        jobject /* this */,
        jlong matAddr) {

    // Use the correct format specifier %lld for jlong
    LOGD("Processing frame at address: %lld", matAddr);

    // Get the input Mat from the address
    cv::Mat& input = *(cv::Mat*)matAddr;
    cv::Mat output;

    // Convert to grayscale
    cv::cvtColor(input, output, cv::COLOR_RGBA2GRAY);

    // Apply Canny edge detection
    cv::Canny(output, output, 50, 150);

    // Convert back to RGBA for display
    cv::cvtColor(output, output, cv::COLOR_GRAY2RGBA);

    // Return the address of the new Mat (heap-allocated)
    return (jlong) new cv::Mat(output);
}

/**
 * Releases the memory of the Mat object created in C++.
 * This function MUST be called from the Java/Kotlin side to prevent memory leaks.
 */
extern "C" JNIEXPORT void JNICALL
Java_com_suyash_edgeviewer_ImageProcessor_releaseMat(
        JNIEnv* env,
        jobject /* this */,
        jlong matAddr) {
    if (matAddr != 0) {
        delete (cv::Mat*)matAddr;
        LOGD("Released Mat at address: %lld", matAddr);
    }
}


// OPTIONAL: Keep this test function if it exists
extern "C" JNIEXPORT jstring JNICALL
Java_com_suyash_edgeviewer_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "OpenCV is working!";
    return env->NewStringUTF(hello.c_str());
}
