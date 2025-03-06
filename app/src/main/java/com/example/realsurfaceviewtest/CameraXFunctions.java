package com.example.realsurfaceviewtest;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.media.Image;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.OptIn;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static android.content.ContentValues.TAG;

public class CameraXFunctions {
    Context context;

    PreviewView previewView;
    LifecycleCameraController cameraController;


    private Bitmap Frame;
    ArrayList<Bitmap> Frames;
    DisplayMetrics displayMetrics;
    public CameraXFunctions(Context context, ActivityResultLauncher<String> launcher){
        this.context = context;
        Frames = new ArrayList<>();
        permissiongranted(launcher);
        displayMetrics = new DisplayMetrics();
        // Get the display metrics from the current window manager
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);


    }

    public void permissiongranted(ActivityResultLauncher<String> getCamera){
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Already granted", Toast.LENGTH_SHORT).show();
        } else {
            getCamera.launch(Manifest.permission.CAMERA);
        }
    }

    public void StartCameraPreview(){
        Size targetResolution = new Size(1280, 720); // Width x Height
         previewView = new PreviewView(context);
         cameraController = new LifecycleCameraController(context);
         cameraController.bindToLifecycle((LifecycleOwner) context);
         cameraController.setCameraSelector(CameraSelector.DEFAULT_BACK_CAMERA);
         previewView.setController(cameraController);

    }

    public PreviewView returnpreview(){
        previewView.post(() -> {


            Size targetResolution = new Size(400, 400); // Width x Height

            Preview preview = new Preview.Builder()
                    .setTargetResolution(targetResolution)
                    .build();


            preview.setSurfaceProvider(previewView.getSurfaceProvider());
        });
        return previewView;
    }

    public void startCamera() {
        // Implementation goes here
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(context);

        //guesing this auto loops
        cameraProviderFuture.addListener(() -> {
            try {
                // Used to bind the lifecycle of cameras to the lifecycle owner
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                // Select back camera
                CameraSelector cameraSelector =
                        new CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                .build();

                // Set up ImageAnalysis
                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                .setTargetResolution(new Size(800, 1400))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();


                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), image -> {
                        Frame = ImageProxtoBitmap2(image);

                    image.close();
                });

                /*imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), image -> {

                    image.close(); // Don't forget to close the image!
                });
                 */

                cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector,  imageAnalysis);
            } catch (Exception exc) {
                Log.e(TAG, "Use case binding failed", exc);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private Bitmap processImage(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] byteArray = new byte[buffer.remaining()];
        buffer.get(byteArray);
        return  bufferToBitmap(buffer,600, 1200);
    }

    private Bitmap bufferToBitmap(ByteBuffer buffer, int width, int height) {
        buffer.rewind();  // Reset buffer position to the beginning
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        return bitmap;
    }

    public Bitmap getFrame() {
        return Frame;
    }

    public Bitmap ImageProxtoBitmap2(ImageProxy imageProxy){

        Bitmap bitmap = imageProxy.toBitmap();
        return bitmap;
    }




    public Bitmap ImageProxtoBitmap(ImageProxy imageProxy) {
        @OptIn(markerClass = ExperimentalGetImage.class) Image image = imageProxy.getImage();
        if (image == null) return null;

        // Get the YUV data from the first plane
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] yuvData = new byte[buffer.remaining()];
        buffer.get(yuvData);

        // Convert YUV to JPEG
        YuvImage yuvImage = new YuvImage(yuvData, ImageFormat.NV21, imageProxy.getWidth(), imageProxy.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, imageProxy.getWidth(), imageProxy.getHeight()), 100, out);
        byte[] jpegData = out.toByteArray();

        // Decode JPEG byte array to Bitmap
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
        return bitmap;
    }

    private Bitmap yuvToBitmap(byte[] yuvData, int width, int height) {
        // Create a bitmap with the specified width and height
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // Separate the Y, U, and V planes from the YUV_420_888 data
        int[] pixels = new int[width * height];  // Array to store RGB pixels

        // Y plane
        ByteBuffer yBuffer = ByteBuffer.wrap(yuvData, 0, width * height);  // The Y plane is the first full-size portion
        byte[] yPlane = new byte[width * height];
        yBuffer.get(yPlane);

        // U and V planes
        ByteBuffer uvBuffer = ByteBuffer.wrap(yuvData, width * height, (width / 2) * (height / 2) * 2);  // The UV planes come after Y
        byte[] uvPlane = new byte[(width / 2) * (height / 2) * 2];
        uvBuffer.get(uvPlane);

        // Iterate over each pixel in the Y plane
        int pixelIndex = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int yIndex = y * width + x;  // Y plane index
                int uIndex = (y / 2) * (width / 2) + (x / 2) * 2;  // U plane index
                int vIndex = uIndex + 1;  // V plane index (U and V are interleaved)

                int yValue = yPlane[yIndex] & 0xFF;  // Y component
                int uValue = uvPlane[uIndex] & 0xFF;  // U component
                int vValue = uvPlane[vIndex] & 0xFF;  // V component

                // Convert YUV to RGB using standard conversion formulas
                int r = (int) (yValue + 1.402f * (vValue - 128));
                int g = (int) (yValue - 0.344136f * (uValue - 128) - 0.714136f * (vValue - 128));
                int b = (int) (yValue + 1.772f * (uValue - 128));

                // Clamp RGB values to the range [0, 255]
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));

                // Combine RGB components into a single pixel (ARGB format)
                pixels[pixelIndex++] = (0xFF << 24) | (r << 16) | (g << 8) | b;
            }
        }

        // Set the pixels array to the bitmap
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        return bitmap;
    }

   /* private Bitmap yuvToBitmap(byte[] yuvData, int width, int height) {
        // Convert YUV to Bitmap. This is just an example, you would use a YUV-to-RGB conversion.
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(yuvData));  // Simplified (may need more conversion logic)
        return bitmap;
    }*/
}
