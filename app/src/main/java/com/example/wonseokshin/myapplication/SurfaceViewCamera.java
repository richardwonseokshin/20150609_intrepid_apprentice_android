package com.example.wonseokshin.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by wonseokshin on 6/12/15.
 */
public class SurfaceViewCamera extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private MainActivity mMainActivity;

    boolean mCameraConfigured = false;
    boolean mInPreview = false;
    boolean mFrontFacing = false;

    public SurfaceViewCamera(Context context, MainActivity mainActivity) {
        super(context);
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        mMainActivity = mainActivity;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open();

        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.setPreviewCallback(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mInPreview)
        {
            mCamera.stopPreview();
        }

        Camera.Parameters parameters = mCamera.getParameters();
        Display display = ((WindowManager)mMainActivity.getSystemService(mMainActivity.WINDOW_SERVICE)).getDefaultDisplay();


        if(!mFrontFacing){
            if(display.getRotation() == Surface.ROTATION_0)
            {
                parameters.setPreviewSize(height, width);
                mCamera.setDisplayOrientation(90);
            }

            if(display.getRotation() == Surface.ROTATION_90)
            {
                parameters.setPreviewSize(width, height);
            }

            if(display.getRotation() == Surface.ROTATION_180)
            {
                parameters.setPreviewSize(height, width);
            }

            if(display.getRotation() == Surface.ROTATION_270)
            {
                parameters.setPreviewSize(width, height);
                mCamera.setDisplayOrientation(180);
            }
        }
        else{
            parameters.setPreviewSize(width, height);
            mCamera.setDisplayOrientation(180);
        }





        initPreview(width, height);
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
        mCamera.release();
        mCamera = null;
        System.gc();
    }

    private void startPreview() {

        if (mCameraConfigured && mCamera!=null) {
            mCamera.startPreview();
            mInPreview=true;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21,previewSize.width,previewSize.height, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 80, baos);
        byte[] jdata = baos.toByteArray();

        Bitmap bitmap = BitmapFactory.decodeByteArray(jdata,0,jdata.length);

        int scaleBitmapWidth = 1;
        int scaleBitmapHeight = 1;
        int rotateBitmapClockwiseDegress = 90;

        if(mFrontFacing){
            rotateBitmapClockwiseDegress += 180;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(rotateBitmapClockwiseDegress);

        bitmap = Bitmap.createBitmap(bitmap , 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        //bitmapOriginal.recycle();
        //scaledBitmap.recycle();

        mMainActivity.updateImageViewCameraBitmap(bitmap);
    }

    private void initPreview(int width, int height) {
        if (mCamera!=null && mSurfaceHolder.getSurface()!=null) {
            try {
                mCamera.setPreviewDisplay(mSurfaceHolder);
            }
            catch (Throwable t) {
                Log.e("SurfaceViewCamera", "initPreview() causing issues");
            }

            if (!mCameraConfigured) {
                Camera.Parameters parameters=mCamera.getParameters();
                Camera.Size size=getBestPreviewSize(width, height,parameters);

                if (size!=null) {
                    parameters.setPreviewSize(size.width, size.height);
                    //parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
                    mCamera.setParameters(parameters);
                    mCameraConfigured=true;
                }
            }
        }

    }

    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result=null;
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result==null) {
                    result=size;
                }
                else {
                    int resultArea = result.width * result.height;
                    int newArea=size.width * size.height;
                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }
        return result;
    }
}
