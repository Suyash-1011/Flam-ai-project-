# EdgeViewer: Real-Time Camera Edge Detection on Android

EdgeViewer is an Android application that demonstrates real-time image processing using the device's camera. The app captures the camera feed and uses a native C++ backend with OpenCV to perform Canny edge detection, displaying the result on the screen. Users can toggle between the live, unprocessed camera feed and the edge-detected feed.


<img width="1080" height="2400" alt="Screenshot_20251007_000140" src="https://github.com/user-attachments/assets/fe67496e-5e1c-403c-b7cc-10da251ed306" />
<img width="1080" height="2400" alt="Screenshot_20251007_000152" src="https://github.com/user-attachments/assets/dd6b2506-3024-4785-8ecb-6d11d11eb80b" />



## Features

*   **Live Camera Feed:** Displays a real-time feed from the device's camera.
*   **Real-Time Edge Detection:** Processes each camera frame to detect and display edges using the Canny algorithm.
*   **Native C++ Integration (JNI):** Image processing logic is written in C++ for maximum performance and executed via the Java Native Interface (JNI).
*   **Dynamic Mode Toggling:** A simple button allows the user to switch between the "Raw Feed" and "Edge Detection ON" modes instantly.
*   **Performance Monitoring:** An on-screen display shows the current Frames Per Second (FPS) to measure processing performance.
*   **Robust Memory Management:** Implements careful memory handling between the Java/Kotlin layer and the native C++ layer to prevent crashes and memory leaks.

## Project Architecture

The application follows a standard Android structure but with a clear separation between the UI/Lifecycle management (in Kotlin) and the heavy computational logic (in C++).

1.  **`MainActivity.kt` (Kotlin - App Layer):**
    *   Manages the Android Activity lifecycle (`onCreate`, `onResume`, `onPause`, `onDestroy`).
    *   Handles camera permission requests.
    *   Initializes and controls the `CameraBridgeViewBase` from the OpenCV SDK, which provides camera frames.
    *   Manages UI elements like the toggle button and FPS counter.
    *   Acts as the `CvCameraViewListener2` to receive frames from the camera.

2.  **`ImageProcessor.kt` (Kotlin - Bridge):**
    *   A simple wrapper class that defines the `native` methods (`processFrame`, `releaseMat`).
    *   It acts as the JNI bridge, loading the native `edgeviewer-lib` library and linking the Kotlin calls to the C++ functions.

3.  **`native-lib.cpp` (C++ - Native Layer):**
    *   This is where the core image processing happens.
    *   It receives the memory address of a camera frame (`Mat`) from the Kotlin side.
    *   **`processFrame` function:**
        1.  Converts the incoming RGBA frame to grayscale.
        2.  Applies the **Canny edge detection** algorithm.
        3.  Converts the single-channel (grayscale) edge map back to a 4-channel RGBA format so it can be displayed.
        4.  Creates a **new** `Mat` object on the native heap to store the result and returns its memory address back to Kotlin.
    *   **`releaseMat` function:**
        1.  A crucial function that accepts a memory address from Kotlin.
        2.  It `delete`s the `Mat` object that was created in `processFrame`, freeing the memory on the native heap and preventing memory leaks.

### How it Works: The Frame Processing Lifecycle

1.  The `CameraBridgeViewBase` captures a frame and passes it to `onCameraFrame` in `MainActivity`.
2.  `MainActivity` gets the `nativeObjAddr` (the memory address) of the frame.
3.  This address is passed to the native `processFrame` function in C++.
4.  C++ performs the edge detection and allocates new memory for the processed `Mat`, returning its new address.
5.  Back in `MainActivity`, a temporary `Mat` wrapper (`tempMat`) is created using this new address.
6.  The data from `tempMat` is copied into a pre-allocated, stable `Mat` object (`processedMat`). **This copy is the key to preventing crashes.**
7.  Crucially, `processor.releaseMat()` is called immediately to free the C++ memory associated with `processedAddr`.
8.  The stable `processedMat` is then returned to the `CameraBridgeViewBase` for rendering on the screen.



