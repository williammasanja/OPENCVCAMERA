package com.example.realsurfaceviewtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.FaceDetectorYN;

import java.io.IOException;
import java.io.InputStream;

import static android.content.ContentValues.TAG;


public class CvFunctions {
    public Context context;
    byte[] buffer;
    MatOfByte mModelBuffer;
    MatOfByte mConfigBuffer;
    FaceDetectorYN mFaceDetector;
    Mat   mFaces;
    float mScale = 3f;

    private static final Scalar    BOX_COLOR         = new Scalar(0, 255, 0);
    private static final Scalar    RIGHT_EYE_COLOR   = new Scalar(255, 0, 0);
    private static final Scalar    LEFT_EYE_COLOR    = new Scalar(0, 0, 255);
    private static final Scalar    NOSE_TIP_COLOR    = new Scalar(0, 255, 0);
    private static final Scalar    MOUTH_RIGHT_COLOR = new Scalar(255, 0, 255);
    private static final Scalar    MOUTH_LEFT_COLOR  = new Scalar(0, 255, 255);

    private Mat                    mRgba;
    private Mat                    mBgr;
    private Mat                    mBgrScaled;


    boolean setup;
    int numFaces;
    Size mInputSize = null;
    public CvFunctions(Context context){
        this.context = context;
        numFaces = 0;

        try {
            InputStream faceML = context.getResources().openRawResource(R.raw.face_detection_yunet_2023mar);
            int size = faceML.available();
            buffer = new byte[size];

            int bytesRead = faceML.read(buffer);
            faceML.close();

        }
        catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to ONNX model from resources! Exception thrown: " + e);
            (Toast.makeText(context, "Failed to ONNX model from resources!", Toast.LENGTH_LONG)).show();
            return;
        }


    }


    public Bitmap getImageFile(int ID){
        Bitmap image = BitmapFactory.decodeResource(context.getResources(), ID);
        return  image;
    }

    public Mat BitmaptoMat(Bitmap bitmap){
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        return mat;
    }

    public Bitmap MattoBitmap(Mat mat) {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);
        return bitmap;
    }

    public Bitmap grayscaleImage(int ID){
        Bitmap normal = getImageFile(ID);
        Mat conversion = BitmaptoMat(normal);
        Mat grayscale = new Mat();
        //Imgproc.cvtColor(conversion,grayscale, Imgproc.COLOR_BGR2RGB);
        Imgproc.cvtColor(conversion, grayscale,Imgproc.COLOR_BGR2GRAY);

        return  MattoBitmap(grayscale);
    }

    private boolean initializedFaceDetector(){
        mModelBuffer = new MatOfByte(buffer);
        mConfigBuffer = new MatOfByte();
        mFaceDetector = FaceDetectorYN.create("onnx", mModelBuffer, mConfigBuffer, new Size(320, 320));
        if (mFaceDetector == null) {
            Log.e(TAG, "Failed to create FaceDetectorYN!");
            (Toast.makeText(context, "Failed to create FaceDetectorYN!", Toast.LENGTH_LONG)).show();
            return false;
        } else
            Log.i(TAG, "FaceDetectorYN initialized successfully!");
        return true;

    }

    public Mat visualize(Mat rgba, Mat faces) {

        int thickness = 2;
        float[] faceData = new float[faces.cols() * faces.channels()];
        numFaces = faces.rows();

        for (int i = 0; i < faces.rows(); i++)
        {
            faces.get(i, 0, faceData);
            Log.d(TAG, "Detected face (" + faceData[0] + ", " + faceData[1] + ", " +
                    faceData[2] + ", " + faceData[3] + ")");

            // Draw bounding box
            Imgproc.rectangle(rgba, new Rect(Math.round(mScale*faceData[0]), Math.round(mScale*faceData[1]),
                            Math.round(mScale*faceData[2]), Math.round(mScale*faceData[3])),
                    BOX_COLOR, thickness);
            // Draw landmarks
            /*Imgproc.circle(rgba, new Point(Math.round(mScale*faceData[4]), Math.round(mScale*faceData[5])),
                    2, RIGHT_EYE_COLOR, thickness);
            Imgproc.circle(rgba, new Point(Math.round(mScale*faceData[6]), Math.round(mScale*faceData[7])),
                    2, LEFT_EYE_COLOR, thickness);
            Imgproc.circle(rgba, new Point(Math.round(mScale*faceData[8]), Math.round(mScale*faceData[9])),
                    2, NOSE_TIP_COLOR, thickness);
            Imgproc.circle(rgba, new Point(Math.round(mScale*faceData[10]), Math.round(mScale*faceData[11])),
                    2, MOUTH_RIGHT_COLOR, thickness);
            Imgproc.circle(rgba, new Point(Math.round(mScale*faceData[12]), Math.round(mScale*faceData[13])),
                    2, MOUTH_LEFT_COLOR, thickness);
            */
        }
        return rgba;
    }

    public Mat onImageFrame(Bitmap image) {
        setup();
        mRgba = BitmaptoMat(image);

        mInputSize = new Size(Math.round(mRgba.cols()/mScale), Math.round(mRgba.rows()/mScale));
        mFaceDetector.setInputSize(mInputSize);

        Imgproc.cvtColor(mRgba, mBgr, Imgproc.COLOR_RGBA2BGR);
        Imgproc.resize(mBgr, mBgrScaled, mInputSize);

        MatOfRect mFaces = new MatOfRect();
        if (mFaceDetector != null) {
            int status = mFaceDetector.detect(mBgrScaled, mFaces);
            Log.d(TAG, "Detector returned status " + status);
            visualize(mRgba, mFaces);
        }

        return mRgba;

    }

    public Bitmap LocateCameraFace(Bitmap bitmap){
        setup = initializedFaceDetector();

        Mat mat = new Mat();
        if(setup) {
            mat = onImageFrame(bitmap);
        }
        return MattoBitmap(mat);
        //6return  bitmap;
    }

    private void setup(){
        mRgba = new Mat();
        mBgr = new Mat();
        mBgrScaled = new Mat();
        mFaces = new Mat();

    }

    public int FacesFound(){
        return numFaces;
    }

    //ALWAYS RELEASE AFTER EACH FRAME
    public void release(){
        mRgba.release();
        mBgr.release();
        mBgrScaled.release();
        mFaces.release();
    }



}
