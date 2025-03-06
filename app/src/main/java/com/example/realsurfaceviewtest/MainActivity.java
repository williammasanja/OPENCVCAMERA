package com.example.realsurfaceviewtest;

import android.graphics.Color;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import org.opencv.android.OpenCVLoader;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity{

    public ActivityResultLauncher<String> getCamera = (
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                Toast.makeText(this, "All set!", Toast.LENGTH_SHORT).show();
            })
    );

    SView surfaceview;
    CameraXFunctions cameraXFunctions;
    CvFunctions cvFunctions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cvFunctions = new CvFunctions(this);
        cameraXFunctions = new CameraXFunctions(this, getCamera);

        surfaceview = new SView(this, cameraXFunctions, cvFunctions);
        surfaceview.setBackgroundColor(Color.RED); //Idk why this works but keep it
        //Adding OpenCv from native code
        if(OpenCVLoader.initLocal()){
            Log.i(TAG, "OpenCV loaded successfully");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
        }


        cameraXFunctions.startCamera();



        setContentView(surfaceview);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (surfaceview!= null) {
            surfaceview.stopThread(); // Stop the thread when the activity is destroyed
        }
    }

}