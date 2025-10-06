#include <jni.h>
#include <string>
#include <opencv2/core.hpp>
#include "android/log.h"

#define TAG "NativeLib"
#define LOGD(...) __android_log_print(ANDROIF_LOG_DEBUG, TAG, __VA_ARGS__)


extern "C" JNIEXPORT jlong JNICALL
Java_com_suyash_edgeviewer_ImageProcessor_processFrame(
        JNIEnv* env,
        jobject /* this */,
        jlong matAddr) {  // <-- THIS WAS MISSING!

    // Log for debugging
    LOGD("Processing frame at address: %ld", matAddr);

    // Get the input Mat from the address
    cv::Mat& input = *(cv::Mat*)matAddr;
    cv::Mat output;

    // Convert to grayscale
    cv::cvtColor(input, output, cv::COLOR_RGBA2GRAY);

    // Apply Canny edge detection
    cv::Canny(output, output, 50, 150);

    // Convert back to RGBA for display
    cv::cvtColor(output, output, cv::COLOR_GRAY2RGBA);

    // Return the address of the new Mat
    return (jlong) new cv::Mat(output);
}

// OPTIONAL: Keep this test function if it exists
extern "C" JNIEXPORT jstring JNICALL
Java_com_suyash_edgeviewer_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "OpenCV is working!";
    return env->NewStringUTF(hello.c_str());
}