package edu.temple.mobilecv;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.tensorflow.demo.env.ImageUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static edu.temple.mobilecv.Constants.DEBUG_TAG;
import static edu.temple.mobilecv.Constants.DEFAULT_HEIGHT;
import static edu.temple.mobilecv.Constants.DEFAULT_WIDTH;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private Button takePictureButton;
    private TextureView textureView;

    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    protected CameraDevice camera;
    private String cameraID;

    private Size imageDimension;
    private int previewWidth = DEFAULT_WIDTH, previewHeight = DEFAULT_HEIGHT;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    //
    //      Texture listener ... displays the camera view for user
    //
    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) { showCameraView(); }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) { }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) { }
    };

    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    //
    //      Camera state callback handler ... responds to changes in camera device state
    //
    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            camera = cameraDevice;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) { camera.close(); }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            camera.close();
            camera = null;
        }
    };

    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    //
    //      Primary activity methods
    //
    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = (TextureView) findViewById(R.id.texture);
        textureView.setSurfaceTextureListener(textureListener);

        takePictureButton = (Button) findViewById(R.id.btn_takepicture);
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { takePicture(); }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable()) showCameraView();
        else textureView.setSurfaceTextureListener(textureListener);
    }

    @Override
    protected void onPause() {
        stopBackgroundThread();
        super.onPause();
    }

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------
    //
    //      Camera interface methods
    //
    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------

    private void showCameraView() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraID = manager.getCameraIdList()[0]; // get primary camera
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraID);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                    PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CAMERA_PERMISSION);
                return;
            }

            manager.openCamera(cameraID, stateCallback, null);
        } catch (CameraAccessException | SecurityException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    protected void takePicture() {
        if (camera == null) {
            Log.e(Constants.DEBUG_TAG, "Camera device is null.");
            return;
        }

        try {
            ImageReader reader = ImageReader.newInstance(DEFAULT_WIDTH, DEFAULT_HEIGHT, ImageFormat.YUV_420_888, 1);
            Surface readerSurface = reader.getSurface();

            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(readerSurface);
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder =
                    camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(readerSurface);
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            final int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            String mobileCvPath = Environment.getExternalStorageDirectory() + "/mobileCV/";
            File mobileCvDir = new File(mobileCvPath);
            if (!mobileCvDir.exists()) mobileCvDir.mkdir();

            String timestampedFilePath = mobileCvPath + new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
            final File imageFile = new File(timestampedFilePath + ".jpg");
            final File csvFile = new File(timestampedFilePath + ".csv");
            final ProgressDialog dialog = new ProgressDialog(this);

            dialog.setTitle("Please wait...");
            dialog.setMessage("Classifying image.  Please wait...");

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.show();
                        }
                    });

                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        Image.Plane[] planes = image.getPlanes();

                        byte[][] yuvBytes = new byte[3][];
                        fillBytes(planes, yuvBytes);
                        saveCsv(yuvBytes[0], yuvBytes[1], yuvBytes[2],
                                planes[0].getRowStride(),
                                planes[1].getRowStride(),
                                planes[1].getPixelStride());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) image.close();
                    }
                }

                private void saveCsv(byte[] yData, byte[] uData, byte[] vData, int yRowStride,
                                     int uvRowStride, int uvPixelStride) throws IOException {
                    int[] rgbBytes = new int[previewWidth * previewHeight];
                    ImageUtils.convertYUV420ToARGB8888(yData, uData, vData,
                            previewWidth, previewHeight,
                            yRowStride, uvRowStride, uvPixelStride,
                            rgbBytes);

                    BufferedWriter br = new BufferedWriter(new FileWriter(csvFile));
                    StringBuilder sb = new StringBuilder();
                    for (int rgbByte : rgbBytes) {
                        sb.append(rgbByte);
                        sb.append(Constants.COMMA);
                    }

                    br.write(sb.toString());
                    br.close();

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                        }
                    });

                    Intent intent = new Intent(MainActivity.this, ClassifierActivity.class);
                    intent.putExtra(Constants.EXTRA_ROTATION, rotation);
                    intent.putExtra(Constants.EXTRA_CSV_PATH, csvFile.getAbsolutePath());
                    startActivity(intent);
                }
            };

            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(MainActivity.this, "Saved:" + csvFile, Toast.LENGTH_LONG).show();
                    createCameraPreview();
                }
            };

            camera.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);

            captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            camera.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (camera == null) return;
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Configuration failed.", Toast.LENGTH_LONG).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void updatePreview() {
        if (camera == null) {
            Log.e(DEBUG_TAG, "Camera device is null.");
            return;
        }

        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }
}