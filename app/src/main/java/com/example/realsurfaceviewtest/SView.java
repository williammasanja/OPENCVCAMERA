package com.example.realsurfaceviewtest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.opengl.GLSurfaceView;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class SView extends SurfaceView implements Runnable{

    Thread thread;
    boolean running;
    private float circleX = 0f; // For simple animation
    Paint P;


    CameraXFunctions cameraXFunctions;
    CvFunctions cvFunctions;
    Bitmap Frame;
    Bitmap CopyFrame; //copies the frame and sets it up when bitmap is null;
    public SView(Context context, CameraXFunctions cameraXFunctions, CvFunctions cvFunctions){
        super(context);
        P = new Paint();
        running = true;
        this.cameraXFunctions = cameraXFunctions;
        this.cvFunctions = new CvFunctions(context);
        thread = new Thread(this);

        thread.start();

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw a circle
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = Math.min(centerX, centerY) / 2f;
        canvas.drawColor(0xFFFFFFFF); // White background

        //canvas.drawCircle(circleX, getHeight() , 50, P);

        String text = "Test";
        P.setTextSize(120);
        canvas.drawText(String.valueOf(cvFunctions.numFaces), 0, getHeight(), P);


        if(cameraXFunctions.getFrame() != null){
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Frame = cvFunctions.LocateCameraFace(cameraXFunctions.getFrame());
            cvFunctions.release();
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(Frame,Frame.getWidth(), Frame.getHeight(), true);

            Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

            canvas.drawBitmap(rotatedBitmap, 0, 0 , P);


        }



//        canvas.drawBitmap(cameraXFunctions.getFrame(), 0, 0 , P);
        //canvas.drawBitmap(getImageFile(R.drawable.img), 0, 0, P);
       // canvas.drawBitmap(Frame, 0, 0, P);

    }


    public Bitmap getImageFile(int ID){
        Bitmap image = BitmapFactory.decodeResource(super.getContext().getResources(), ID);
        return  image;
    }

    @Override
    public void run() {
        while(running){


            postInvalidate(); // Redraw the view
            //Frame = cvFunctions.LocateCameraFace(cameraXFunctions.getFrame());

            try {
                Thread.sleep((long) 33.67); // Control frame rate (~30 FPS)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public void stopThread() {
        running = false;
        try {
            thread.join(); // Wait for the thread to finish
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
