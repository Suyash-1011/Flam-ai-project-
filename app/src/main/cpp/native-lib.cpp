#include <jni.h>
#include <string>
#include <opencv2/core.hpp>
extern "C" JNIEXPORT jstring JNICALL
Java_com_suyash_edgeviewer_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    cv::Mat test = cv::Mat::eye(3, 3, CV_8UC1);
    std::string msg = "OpenCV works! Mat size: " + std::to_string(test.rows) + "x" + std::to_string(test.cols);
    return env->NewStringUTF(msg.c_str());
}