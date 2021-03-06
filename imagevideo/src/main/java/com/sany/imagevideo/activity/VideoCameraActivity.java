package com.sany.imagevideo.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
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
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.PowerManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.sany.imagevideo.R;
import com.sany.imagevideo.camera2.AutoFitTextureView;
import com.sany.imagevideo.camera2.CaptureLayout2;
import com.sany.imagevideo.camera2.CaptureListener2;
import com.sany.imagevideo.camera2.CustomVideoView;
import com.sany.imagevideo.camera2.util.BitmapDecoder;
import com.sany.imagevideo.camera2.util.BitmapSize;
import com.sany.imagevideo.camera2.util.Camera2Config;
import com.sany.imagevideo.camera2.util.Camera2Util;
import com.sany.imagevideo.jcamera.listener.ClickListener;
import com.sany.imagevideo.jcamera.listener.TypeListener;
import com.sany.imagevideo.jcamera.util.AngleUtil;
import com.sany.imagevideo.jcamera.util.ContentValue;
import com.sany.imagevideo.jcamera.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class VideoCameraActivity extends Activity {

    private static final String TAG = "VideoCameraActivity";
    private static final String IMAGE_QUALITY = "imageQuality";
    private static final String VIDEO_SECONDS = "videoSeconds";
    private static final String VIDEO_BIT_RATE = "videoBitRate";
    private static final String VIDEO_FRAME_RATE = "videoFrameRate";
    private AutoFitTextureView mTextureView;
    private CaptureLayout2 mCaptureLayout;

    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    private boolean mIsRecordingVideo = false;//????????????????????????
    private boolean isStop = false;//??????????????????MediaRecorder
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    //???????????????
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mPreviewSession;
    private CaptureRequest mCaptureRequest;
    private CaptureRequest.Builder mPreviewBuilder;
    private ImageReader mImageReader;//????????????
    private CameraCharacteristics characteristics;

    private String picSavePath;//??????????????????
    public int widthPixels;
    public int heightPixels;
    private int width;//TextureView??????
    private int height;//TextureView??????
    private String mCameraId;//???????????????ID
    private String mCameraIdFront;//???????????????ID
    private boolean isCameraFront = false;//??????????????????????????????

    private MediaRecorder mMediaRecorder;

    private Size mPreviewSize;//?????????Size
    private Size mCaptureSize;//??????Size
    private Size mVideoSize;//??????size

    private String mNextVideoAbsolutePath;//????????????

    private PowerManager.WakeLock mWakeLock;
    //?????????????????????(x1, y1)???????????????????????????(x2, y2)
    float finger_spacing;
    int zoom_level = 0;
    private Rect zoom;

    private RelativeLayout rl_preview;
    private ImageView iv_preview, iv_switch;
    private CustomVideoView video_preview;
    private Intent resultIntent;

    private int imageQuality = 30;
    private int videoSeconds = 10;
    //??????(?????????)?????????????????????????????????????????????????????????,???????????????????????????kbps??????????????????????????????????????????????????????????????????????????????????????????
    // ????????????????????????????????????????????????????????????????????????????????????????????????????????? ??????????????????????????????????????????????????????????????????????????????????????????
    // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    // ???????????????(kbps)=????????????(??????)X8 /??????(???)/1000
    private int videoBitRate = 1200 * 1280;
    //?????????????????????????????????????????????????????????????????????????????????24???????????????????????????????????????????????????????????????????????????????????????????????????15???????????????????????????????????????????????????????????????????????????????????????
    private int videoFrameRate = 30;

    //????????????
    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_camera);
        init();
        initView();
        initListener();
    }

    private void init() {
        imageQuality = getIntent().getIntExtra(IMAGE_QUALITY, 30);
        videoSeconds = getIntent().getIntExtra(VIDEO_SECONDS, 10);
        videoBitRate = getIntent().getIntExtra(VIDEO_FRAME_RATE, 1200 * 1280);
        videoFrameRate = getIntent().getIntExtra(VIDEO_BIT_RATE, 30);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);  //????????????
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        widthPixels = dm.widthPixels;
        heightPixels = dm.heightPixels;
    }

    private void initView() {
        mTextureView = findViewById(R.id.texture);
        mCaptureLayout = findViewById(R.id.capture_layout);
        int maxRecordTime = videoSeconds * 1000;
        mCaptureLayout.setDuration(maxRecordTime);
        mCaptureLayout.setIconSrc(0, 0);

        rl_preview = findViewById(R.id.rl_preview);
        iv_preview = findViewById(R.id.iv_preview);//????????????
        iv_switch = findViewById(R.id.iv_switch);//???????????????
        video_preview = findViewById(R.id.video_preview);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initListener() {
        //?????? ??????
        mCaptureLayout.setCaptureLisenter(new CaptureListener2() {
            //??????
            @Override
            public void takePictures() {
                mIsRecordingVideo = false;
                //??????????????????????????????
                iv_switch.setVisibility(View.GONE);
                capture();
            }

            //????????????
            @Override
            public void recordStart() {
                iv_switch.setVisibility(View.GONE);
                mIsRecordingVideo = true;
                startRecordingVideo();
                mMediaRecorder.start();
                isStop = false;
            }

            //????????????
            @Override
            public void recordShort(final long time) {
                iv_switch.setVisibility(View.GONE);
                mIsRecordingVideo = false;
                Toast.makeText(VideoCameraActivity.this, "????????????????????????????????????", Toast.LENGTH_SHORT).show();
                FileUtil.delFile(mNextVideoAbsolutePath);
                mNextVideoAbsolutePath = null;
                invokeResetDelay();
            }

            //????????????
            @Override
            public void recordEnd(long time) {
                iv_switch.setVisibility(View.GONE);
                mIsRecordingVideo = false;
                if (!isStop) {
                    stopRecordingVideo(false);
                }
                mCaptureLayout.startAlphaAnimation();
                mCaptureLayout.startTypeBtnAnimator();
            }

            //??????
            @Override
            public void recordZoom(Rect zoom) {
                mPreviewBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
                try {
                    mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void recordError() {

            }
        });
        //?????? ??????
        mCaptureLayout.setTypeLisenter(new TypeListener() {
            @Override
            public void cancel() {
                iv_switch.setVisibility(View.VISIBLE);
                rl_preview.setVisibility(View.INVISIBLE);
                iv_preview.setVisibility(View.GONE);
                video_preview.stopPlayback();
                //??????????????????
                showResetCameraLayout();
            }

            @Override
            public void confirm() {
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
        //??????????????????
        mCaptureLayout.setLeftClickListener(this::finish);
        //???????????????????????????????????????
        video_preview.setOnPreparedListener(mp -> {
            mp.start();//????????????
            mp.setLooping(true);
        });
        video_preview.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "MediaPlayer--play error");
            return false;
        });
        //???????????????
        iv_switch.setOnClickListener(v -> switchCamera());
        //???????????????????????????
        mTextureView.setOnTouchListener((view, event) -> {
            //????????????
            changeZoom(event);
            return true;
        });
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int w, int h) {
            //???SurefaceTexture???????????????????????????????????????????????????
            width = w;
            height = h;
            setupCamera(w, h);
            openCamera(mCameraId);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int w, int h) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }

    };

    @SuppressLint({"InvalidWakeLockTag", "WakelockTimeout"})
    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
        if (mWakeLock == null) {
            //???????????????,??????????????????
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
            mWakeLock.acquire();
        }
        //??????????????????????????????????????????
        isCameraFront = false;
    }

    @Override
    public void onPause() {
        if (mIsRecordingVideo) {
            stopRecordingVideo(true);
        }
        closeCamera();
        stopBackgroundThread();
        super.onPause();
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

	/*private void closePreviewSession() {
		if (mPreviewSession != null) {
			mPreviewSession.close();
			mPreviewSession = null;
		}
	}*/

    /**
     * **********************************************???????????????**************************************
     */
    public void switchCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

        if (isCameraFront) {
            isCameraFront = false;
            setupCamera(width, height);
            openCamera(mCameraId);
        } else {
            isCameraFront = true;
            setupCamera(width, height);
            openCamera(mCameraIdFront);
        }
    }

    /**
     * ***************************** ???????????????????????? *****************************************
     */
    public void invokeResetDelay() {
        mCaptureLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                showResetCameraLayout();
            }
        }, 500);
    }

    public void showResetCameraLayout() {
        resetCamera();
        rl_preview.setVisibility(View.INVISIBLE);
        video_preview.stopPlayback();
    }

    //????????????????????????
    public void resetCamera() {
        if (TextUtils.isEmpty(mCameraId)) {
            return;
        }
        closeCamera();
        mCaptureLayout.resetCaptureLayout();
        setupCamera(width, height);
        if (isCameraFront) {
            openCamera(mCameraIdFront);
        } else {
            openCamera(mCameraId);
        }
    }

    /**
     * ******************************** ????????????????????? *********************************
     * Tries to open a {@link CameraDevice}. The result is listened by mStateCallback`.
     */
    private void setupCamera(int width, int height) {

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            //0?????????????????????,1?????????????????????
            mCameraId = manager.getCameraIdList()[0];
            mCameraIdFront = manager.getCameraIdList()[1];

            //????????????????????????????????????????????????????????????????????????????????????
            if (isCameraFront) {
                characteristics = manager.getCameraCharacteristics(mCameraIdFront);
            } else {
                characteristics = manager.getCameraCharacteristics(mCameraId);
            }
            mCaptureLayout.setCharacteristics(characteristics);

            // Choose the sizes for camera preview and video recording
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            //Integer mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class), width, height);
            mPreviewSize = Camera2Util.getMinPreSize(map.getOutputSizes(SurfaceTexture.class), width, height, Camera2Config.PREVIEW_MAX_HEIGHT);

            //???????????????????????????????????????
            mCaptureSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getHeight() * rhs.getWidth());
                }
            });

            configureTransform(width, height);

            //???ImageReader??????????????????
            setupImageReader();

            mMediaRecorder = new MediaRecorder();
        } catch (CameraAccessException e) {
            //UIUtil.toastByText("Cannot access the camera.", Toast.LENGTH_SHORT);
            Toast.makeText(this, "?????????????????????.", Toast.LENGTH_SHORT).show();
            finish();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            //UIUtil.toastByText("This device doesn't support Camera2 API.", Toast.LENGTH_SHORT);
            Toast.makeText(this, "??????????????? Camera2 API.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * *********************************??????ImageReader,??????????????????****************************
     */
    @SuppressLint("HandlerLeak")
    private void setupImageReader() {
        //2??????ImageReader????????????????????????????????????
        mImageReader = ImageReader.newInstance(mCaptureSize.getWidth(), mCaptureSize.getHeight(), ImageFormat.JPEG, 2);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image mImage = reader.acquireNextImage();
                ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);

                Log.e(TAG, "onImageAvailable: data.length " + data.length);


                FileUtil.createSavePath(ContentValue.getImagePath(VideoCameraActivity.this));//?????????????????????????????????????????????????????????
                String picSavePath1 = ContentValue.getImagePath(VideoCameraActivity.this) + "IMG_" + System.currentTimeMillis() + ".jpg";

                try {
                    FileOutputStream out = new FileOutputStream(picSavePath1);
                    out.write(data, 0, data.length);
                    //??????????????????bitmap
                    File captureImage = new File(picSavePath1);
                    Bitmap bitmap = BitmapDecoder.decodeSampledBitmapFromFile(captureImage, new BitmapSize(widthPixels, heightPixels), Bitmap.Config.RGB_565);
                    picSavePath = ContentValue.getImagePath(VideoCameraActivity.this) + "IMG_" + System.currentTimeMillis() + ".jpg";
                    FileOutputStream out2 = new FileOutputStream(picSavePath);
                    if (bitmap != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, imageQuality, out2);
                        //???????????????
                        if (captureImage.isFile() && captureImage.exists()) {
                            captureImage.delete();
                        }
                    }
                    Message msg = new Message();
                    msg.what = 0;
                    msg.obj = picSavePath;
                    mBackgroundHandler.sendMessage(msg);
                    out.flush();
                    out2.flush();
                    out.close();
                    out2.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, mBackgroundHandler);

        mBackgroundHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 0:
                        //??????????????????????????????????????????????????????
                        rl_preview.setVisibility(View.VISIBLE);
                        //????????????
                        try {
                            File captureImage = new File(picSavePath);
                            Bitmap bitmap = BitmapDecoder.decodeSampledBitmapFromFile(captureImage, new BitmapSize(widthPixels, heightPixels), Bitmap.Config.RGB_565);
                            iv_preview.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        iv_preview.setVisibility(View.VISIBLE);
                        video_preview.setVisibility(View.GONE);
                        //????????????????????????
                        resultIntent = new Intent();
                        resultIntent.putExtra("isPhoto", true);
                        resultIntent.putExtra("imageUrl", picSavePath);

                        mCaptureLayout.startAlphaAnimation();
                        mCaptureLayout.startTypeBtnAnimator();

                        Log.d(TAG, "??????????????????");
                        break;
                }
            }
        };
    }

    /**
     * @param //????????????????????????ImageView????????????Bitmap??????
     * @param bytes
     * @param opts
     * @return Bitmap
     */
    public Bitmap getPicFromBytes(byte[] bytes,
                                  BitmapFactory.Options opts) {
        if (bytes != null)
            if (opts != null)
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length,
                        opts);
            else
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        return null;
    }

    /**
     * ******************************openCamera(??????Camera)*****************************************
     */
    @SuppressLint("MissingPermission")
    private void openCamera(String CameraId) {
        //???????????????????????????CameraManager
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        //????????????
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            //???????????????????????????????????????????????????????????????????????????stateCallback????????????????????????????????????????????????????????????Callback???????????????????????????null??????????????????????????????
            manager.openCamera(CameraId, mStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //?????????????????????
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            startPreview();
            if (null != mTextureView) {
                configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
            }
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            mCameraDevice = null;
        }
    };

    /**
     * ************************************* ????????????. **********************************
     */
    private void startPreview() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        if (texture == null) {
            return;
        }
        try {
            //closePreviewSession();
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            Surface previewSurface = new Surface(texture);
            mPreviewBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    //??????????????????
                    mCaptureRequest = mPreviewBuilder.build();
                    mPreviewSession = cameraCaptureSession;
                    //??????????????????????????????????????????????????????
                    try {
                        mPreviewSession.setRepeatingRequest(mCaptureRequest, null, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(VideoCameraActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * ***************************** ??????????????? ******************************
     */
    private void closeCamera() {
        try {
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mMediaRecorder) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ********************************************??????*********************************************
     */
    private void capture() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            CaptureRequest.Builder mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            //??????????????????
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            mCaptureBuilder.addTarget(mImageReader.getSurface());
            //isCameraFront?????????????????????boolean??????????????????????????????????????????????????????????????????180??????????????????????????????????????
            if (isCameraFront) {
                mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, DEFAULT_ORIENTATIONS.get(Surface.ROTATION_180));
            } else {
                mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, DEFAULT_ORIENTATIONS.get(rotation));
            }

            //????????????
            mCaptureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);

            //??????????????????????????????????????????,??????????????????????????????????????????
            mCaptureBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);

            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    //?????????unLockFocus
                    unLockFocus();
                }
            };
            mPreviewSession.stopRepeating();
            //????????????
            mPreviewSession.capture(mCaptureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unLockFocus() {
        try {
            // ????????????AF?????????
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            //?????????????????????????????????
            mPreviewBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            //??????????????????
            mPreviewSession.setRepeatingRequest(mCaptureRequest, null, mBackgroundHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ***************************** ?????? ?????? START *******************************************
     */
    //????????????
    private void startRecordingVideo() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            Toast.makeText(this, "????????????", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        try {
            //closePreviewSession();
            setUpMediaRecorder();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();

            // Set up Surface for the camera preview
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);

            // Set up Surface for the MediaRecorder
            Surface mRecorderSurface = mMediaRecorder.getSurface();
            surfaces.add(mRecorderSurface);
            mPreviewBuilder.addTarget(mRecorderSurface);

            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mCaptureRequest = mPreviewBuilder.build();
                    mPreviewSession = cameraCaptureSession;
                    try {
                        mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(VideoCameraActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }
    }

    //????????????
    private void stopRecordingVideo(boolean shortTime) {
        mIsRecordingVideo = false;
        try {
            if (mPreviewSession != null) {
                mPreviewSession.stopRepeating();
                mPreviewSession.abortCaptures();
            }
            mBackgroundHandler.removeCallbacksAndMessages(null);
            mMediaRecorder.setOnErrorListener(null);
            mMediaRecorder.setOnInfoListener(null);
            mMediaRecorder.setPreviewDisplay(null);
            mMediaRecorder.stop();
            // Stop recording
            mMediaRecorder.reset();
            isStop = true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!shortTime) {
            Log.d(TAG, "????????????");
            resultIntent = new Intent();
            resultIntent.putExtra("videoPath", mNextVideoAbsolutePath);
            boolean isOK = FileUtil.getVideoWH(mNextVideoAbsolutePath, resultIntent);
            if (isOK) {
                video_preview.setVisibility(View.VISIBLE);
                iv_preview.setVisibility(View.GONE);
                video_preview.setVideoPath(mNextVideoAbsolutePath);
                video_preview.requestFocus();
                video_preview.start();
                rl_preview.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "???????????????????????????????????????????????????", Toast.LENGTH_SHORT).show();
                invokeResetDelay();
            }
        } else {//?????????????????????????????????
            Toast.makeText(this, "????????????????????????????????????", Toast.LENGTH_SHORT).show();
            FileUtil.delFile(mNextVideoAbsolutePath);
            mNextVideoAbsolutePath = null;
            invokeResetDelay();
        }
    }

    /**
     * ????????????
     *
     * @throws IOException
     */
    private void setUpMediaRecorder() throws IOException {
        mMediaRecorder.reset();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        if (mNextVideoAbsolutePath == null || mNextVideoAbsolutePath.isEmpty()) {
            mNextVideoAbsolutePath = getVideoFilePath();
        }

        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncodingBitRate(videoBitRate);
        mMediaRecorder.setVideoFrameRate(videoFrameRate);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);

        //??????????????????????????????,????????????????????????????????????
        if (isCameraFront) {
            mMediaRecorder.setOrientationHint(270);
        } else {
            mMediaRecorder.setOrientationHint(90);
        }

        mMediaRecorder.prepare();
    }
    /**
     * ***************************** ?????? ?????? END *******************************************
     */

	/*private void updatePreview() {
		if (null == mCameraDevice) {
			return;
		}
		try {
			setUpCaptureRequestBuilder(mPreviewBuilder);
			if(mBackgroundHandler!=null&&!isClose) {
				HandlerThread thread = new HandlerThread("CameraPreview");
				thread.start();
				mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
			}
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}
	}

	private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
		builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
	}*/

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            if (mBackgroundHandler != null) {
                mBackgroundHandler.removeCallbacksAndMessages(null);
            }
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static Size chooseVideoSize(Size[] choices, int width, int height) {
        for (Size size : choices) {
            float ft = (float) size.getWidth() / (float) size.getHeight();
            if (size.getWidth() <= 1300 && size.getWidth() <= height && ft > 1.5 && ft < 1.9) {
                return size;
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }

    /**
     * ???????????????????????????????????????
     *
     * @param viewWidth  ??????????????????
     * @param viewHeight ??????????????????
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == mPreviewSize) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    /**
     * *********************************** ???????????? *********************************
     */
    public void changeZoom(MotionEvent event) {
        try {
            //????????????????????????????????????????????????????????????????????????????????????????????????????????????
            float maxZoom = (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)) * 10;
            Rect m = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

            int action = event.getAction();
            float current_finger_spacing;
            //??????????????????????????????
            if (event.getPointerCount() > 1) {
                //??????????????????????????????
                current_finger_spacing = getFingerSpacing(event);

                if (finger_spacing != 0) {
                    if (current_finger_spacing > finger_spacing && maxZoom > zoom_level) {
                        zoom_level++;

                    } else if (current_finger_spacing < finger_spacing && zoom_level > 1) {
                        zoom_level--;
                    }

                    int minW = (int) (m.width() / maxZoom);
                    int minH = (int) (m.height() / maxZoom);
                    int difW = m.width() - minW;
                    int difH = m.height() - minH;
                    int cropW = difW / 100 * (int) zoom_level;
                    int cropH = difH / 100 * (int) zoom_level;
                    cropW -= cropW & 3;
                    cropH -= cropH & 3;
                    zoom = new Rect(cropW, cropH, m.width() - cropW, m.height() - cropH);
                    mPreviewBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
                }
                finger_spacing = current_finger_spacing;
            } else {
                if (action == MotionEvent.ACTION_UP) {
                    //single touch logic,????????????????????????
                }
            }
            try {
                mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                                super.onCaptureCompleted(session, request, result);
                            }
                        },
                        null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            throw new RuntimeException("can not access camera.", e);
        }
    }

    //??????????????????????????????
    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private String getVideoFilePath() {
        return FileUtil.createFilePath(ContentValue.getVideoPath(VideoCameraActivity.this), null, Long.toString(System.currentTimeMillis()));
    }

}
