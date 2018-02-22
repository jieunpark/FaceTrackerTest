/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.samples.vision.face.facetracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.samples.vision.face.facetracker.ui.camera.Camera2Source;
import com.google.android.gms.samples.vision.face.facetracker.ui.camera.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.samples.vision.face.facetracker.ui.camera.CameraSourcePreview;
import com.google.android.gms.samples.vision.face.facetracker.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.text.Line;
import com.waynejo.androidndkgif.GifEncoder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.os.Environment.getExternalStorageDirectory;

/**
 * Activity for the face tracker app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 */
public final class FaceTrackerActivity extends AppCompatActivity {
    private static final String TAG = "FaceTracker";

//    private CameraSource mCameraSource = null;

    private Camera2Source mCamera2Source = null;

    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    private Button btnTakePicture;
    private ImageView imgCapture;
    private LinearLayout layoutRoot;



    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);


        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }

        layoutRoot= (LinearLayout) findViewById(R.id.topLayout);
        imgCapture = (ImageView) findViewById(R.id.imgCapture);
        btnTakePicture = (Button) findViewById(R.id.btnTakePicture);
        btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                takePicture();

                mCamera2Source.setPreviewCallback(mListener2);

                Handler delayHandler = new Handler();
                delayHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mCamera2Source.setPreviewCallback(null);
                    }
                }, 3000);

//                mCameraSource.setPreviewCallback(mListener);
//
//                Handler delayHandler = new Handler();
//                delayHandler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        // TODO
//                        mCameraSource.setPreviewCallback(null);
////                        closeGif();
//
//                        try {
//                            isEncoding = false;
//                            encodeGIF();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }, 3000);

            }
        });


    }

    public static Bitmap viewToBitmap(View view, int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    /**
     * preview 만 캡쳐
     */
    private void takePicture() {
//        mPreview.setDrawingCacheEnabled(true);
//        mPreview.buildDrawingCache();
//        Bitmap bitmap = mPreview.getDrawingCache();
//        imgCapture.setImageBitmap(bitmap);

        imgCapture.setImageBitmap(viewToBitmap(mPreview, mPreview.getWidth(), mPreview.getHeight()));
    }

//    /**
//     * surfaceview캡
//     */
//    private void takePicture2() {
//        imgCapture.setImageBitmap(mPreview.takePicture());
//    }

    /**
     * Camera Resource 캡쳐
     */
//    private void takePicture3() {
//
//        mCameraSource.takePicture(null, new CameraSource.PictureCallback() {
//            @Override
//            public void onPictureTaken(byte[] bytes) {
//                Bitmap pictureBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//
//                Bitmap temp=Bitmap.createBitmap(pictureBitmap.getWidth(),pictureBitmap.getHeight(),pictureBitmap.getConfig());
//                Canvas canvas = new Canvas(temp);
//                canvas.drawBitmap(pictureBitmap,0,0,null);
//
//                mPreview.buildDrawingCache();
//                Bitmap overlayBitmap = mPreview.getDrawingCache();
//                canvas.drawBitmap(overlayBitmap,0,0,null);
//
//
//
//                imgCapture.setImageBitmap(temp);
//            }
//        });
//    }

//    /**
//     * SurfaceView캡쳐
//     */
//    private void takePicture4() {
//        imgCapture.setImageBitmap(mPreview.takePicture());
//    }
//
//    private void takePicture5() {
////        layoutRoot.buildDrawingCache();
////        Bitmap bitmap = layoutRoot.getDrawingCache();
////        imgCapture.setImageBitmap(bitmap);
//
//        imgCapture.setImageBitmap(viewToBitmap(layoutRoot, layoutRoot.getWidth(), layoutRoot.getHeight()));
//    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {

        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

//        mCameraSource = new CameraSource.Builder(context, detector)
//                .setRequestedPreviewSize(640, 480)
//                .setFacing(CameraSource.CAMERA_FACING_BACK)
//                .setRequestedFps(30.0f)
//                .build();

        mCamera2Source = new Camera2Source.Builder(context, detector)
                .setFocusMode(Camera2Source.CAMERA_AF_AUTO)
                .setFlashMode(Camera2Source.CAMERA_FLASH_AUTO)
                .setFacing(Camera2Source.CAMERA_FACING_BACK)
                .build();

    }

    private Camera2Source.OnPreviewListener mListener2 = new Camera2Source.OnPreviewListener() {
        @Override
        public void onPreviewFrame(ImageReader reader) {
            Log.e(TAG, ">>> onPreviewFrame ");

            Image image = reader.acquireLatestImage();
            if(image==null)
                return;
            final byte[] bytes = getYUVbytes(image);
            int width = image.getWidth();
            int height = image.getHeight();
            image.close();

            final Bitmap bitmap = decodeYUVbytes(FaceTrackerActivity.this, bytes, width, height, Camera2Source.CAMERA_FACING_BACK);

            runOnUiThread(new Runnable(){
                @Override
                public void run() {
                    imgCapture.setImageBitmap(bitmap);
                }
            });
        }
    };

    public static Bitmap decodeYUVbytes(Context context, byte[] data, int width, int height, int lensfacing) {

        if (data == null) return null;

        int W = width;
        int H = height;


        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(data.length);
        Allocation in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(W).setY(H);
        Allocation out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);


        Bitmap bmpout = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);

        in.copyFromUnchecked(data);

        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);
        out.copyTo(bmpout);

//        Matrix matrix = new Matrix();
//        int angle = 90;

        Matrix matrix = getYUVMatrix(width, lensfacing);
//        if(lensfacing==CameraCharacteristics.LENS_FACING_BACK)
//            angle = 90;
//        else
//            angle = 270;
//        matrix.preRotate(angle, W/2, H/2);
        bmpout = Bitmap.createBitmap(bmpout, 0, 0, W, H, matrix, false);
        return bmpout ;
    }

    public static Matrix getYUVMatrix(int width, int lensFacing) {
        String deviceManufacture = android.os.Build.MANUFACTURER;
        Matrix matrix = null;
        if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
            if(matrix==null)
                matrix = new Matrix();
            matrix.preScale(-1, 1);
            matrix.preTranslate(width, 0);
            matrix.preRotate(270);      // 전면카메라일때 회전각도
        }
        else {
//        if(deviceManufacture.equals(MANUFACTURER_SAMSUNG)) {
            if(matrix==null)
                matrix = new Matrix();
            matrix.preRotate(90);       // 후면카메라일때 회전각도
        }
        return matrix;
    }

    public static byte[] getYUVbytes(Image image) {
        try {
            Image.Plane Y = image.getPlanes()[0];
            Image.Plane U = image.getPlanes()[1];
            Image.Plane V = image.getPlanes()[2];

            int Yb = Y.getBuffer().remaining();
            int Ub = U.getBuffer().remaining();
            int Vb = V.getBuffer().remaining();

            byte[] data = new byte[Yb + Ub + Vb];

            Y.getBuffer().get(data, 0, Yb);
            V.getBuffer().get(data, Yb, Vb);
            U.getBuffer().get(data, Yb + Vb, Ub);
            return data;
        } catch (Exception e) {
            System.out.println("## Error while get yuv bytes: ");
            e.printStackTrace();
            return null;
        }
    }

    private CameraSource.OnPreviewListener mListener = new CameraSource.OnPreviewListener() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
//            Log.e(TAG,">>> onPreviewFrame data width ");

//            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0 , data.length);
//            imgCapture.setImageBitmap(bitmap);
            Camera.Parameters params = camera.getParameters();

            int w = params.getPreviewSize().width;

            int h = params.getPreviewSize().height;

            int format = params.getPreviewFormat();

            YuvImage image = new YuvImage(data, format, w, h, null);

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Rect area = new Rect(0, 0, w, h);

            image.compressToJpeg(area, 50, out);

            Bitmap captureImg = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size());
            imgCapture.setImageBitmap(captureImg);


            if (isEncoding) {

                bitmapList.add(captureImg);
            }

//            try {
//                    encodeGIF(captureImg);
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }



        }
    };

    List<Bitmap> bitmapList = new ArrayList<>();


    private boolean isEncoding  = true;
    private int frameCount = 0;

    private void encodeGIF() throws IOException {

        int width = 640;
        int height = 480;

        String dstFile = "result.gif";
        final String filePath = Environment.getExternalStorageDirectory() + File.separator + dstFile;

        GifEncoder gifEncoder = new GifEncoder();

        try {
            Log.i(TAG,">>>>>>>>>>bitmap size : "+bitmapList.size());

            gifEncoder.init(width, height, filePath, GifEncoder.EncodingType.ENCODING_TYPE_NORMAL_LOW_MEMORY);
            gifEncoder.setDither(true);

            for(Bitmap bitmap : bitmapList) {
                Log.i(TAG,">>>>>>>>>>encoding");
                gifEncoder.encodeFrame(bitmap, 0);
            }

            gifEncoder.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(FaceTrackerActivity.this, "done : " + filePath, Toast.LENGTH_SHORT).show();
//            }
//        });

    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
                   startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (mCameraSource != null) {
//            mCameraSource.release();
//        }
        if (mCamera2Source != null) {
            mCamera2Source.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Face Tracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCamera2Source != null) {
            try {
                mPreview.start(mCamera2Source, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCamera2Source.release();
                mCamera2Source = null;
            }
        }
    }

    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            Log.e(TAG,">>> Face : " + face);
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }
    }


}
