package org.lqos.zxing.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;

import org.lqos.zxing.camera.CameraManager;
import org.lqos.zxing.encoding.QRCodeDecoder;
import org.lqos.zxing.encoding.Utils;
import org.lqos.zxing.view.ViewfinderView;
import org.lqos.zxings.R;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;

/**
 * Created by Administrator on 2017/9/11 0011.
 */

public class QRCodeFragment extends Fragment implements SurfaceHolder.Callback {
    private static final String TAG = QRCodeFragment.class.getSimpleName();
    public static final String KEY_RESULT = "result";

    private Result savedResultToShow;
    private boolean hasSurface;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private AmbientLightManager ambientLightManager;
    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    SurfaceView surfaceView;
    private Collection<BarcodeFormat> decodeFormats;
    private Map<DecodeHintType, ?> decodeHints;
    private String characterSet;
    private boolean mLightOn = false;


    ViewfinderView getViewfinderView() {
        return viewfinderView;
    }


    CameraManager getCameraManager() {
        return cameraManager;
    }

    public boolean lightOnAndOff() {
        mLightOn = !mLightOn;
        cameraManager.setTorch(mLightOn);
        return mLightOn;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String photo_path = Utils.getSelectMediaPath(getActivity().getApplicationContext(), data);
        if (TextUtils.isEmpty(photo_path)) {
            photo_path = Utils.getPath(getActivity().getApplicationContext(), data.getData());
        }
        new AsyncTasks(this).execute(photo_path);
    }


    static class AsyncTasks extends AsyncTask<String, Void, Result> {
        QRCodeFragment activity;
        String path;

        public AsyncTasks(QRCodeFragment captureActivity) {
            activity = captureActivity;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected Result doInBackground(String... params) {
            Log.e("path - ", "" + params[0]);
            this.path = params[0];
            return QRCodeDecoder.syncDecodeQRCode(path);
        }


        @Override
        protected void onPostExecute(Result result) {
            if (result == null) {
                Toast.makeText(activity.getActivity().getApplicationContext(), "未发现二维码", Toast.LENGTH_SHORT).show();
            } else {
                activity.handleDecode(result, (this.path), -1f);
            }
        }
    }

    public void photo() {

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            Intent innerIntent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            innerIntent.setType("image/*");
            Intent wrapperIntent = Intent.createChooser(innerIntent, "选择二维码图片");
            this.startActivityForResult(wrapperIntent, 234);
        } else {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    0x457);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0x457) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                photo();
            } else {
                Toast.makeText(getActivity().getApplicationContext(), "获取文件读写权限失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = View.inflate(getActivity(), R.layout.activity_capturer, null);
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent == null) {
            initData(savedInstanceState);
        }
        surfaceView = (SurfaceView) view.findViewById(R.id.preview_view);
        viewfinderView = (ViewfinderView) view.findViewById(R.id.viewfinder_view);
        return view;
    }

    private void initData(Bundle savedInstanceState) {
        hasSurface = false;
        inactivityTimer = new InactivityTimer(getActivity());
        beepManager = new BeepManager(getActivity());
        ambientLightManager = new AmbientLightManager(getActivity());
        decodeFormats = EnumSet.noneOf(BarcodeFormat.class);
        decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.ONE_D_FORMATS);
    }

    @Override
    public void onResume() {
        super.onResume();
        initResume();
    }

    private void initPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        ambientLightManager.stop();
        beepManager.close();
        if (cameraManager != null)
            cameraManager.closeDriver();
        if (!hasSurface) {
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        if (mLightOn) {
            lightOnAndOff();
        }
    }

    private void initResume() {
        if (cameraManager == null)
            cameraManager = new CameraManager(getActivity().getApplication());
        viewfinderView.setCameraManager(cameraManager);
        handler = null;
        beepManager.updatePrefs();
        ambientLightManager.start(cameraManager);
        inactivityTimer.onResume();
        characterSet = null;
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
        }
    }

    public void handleDecode(Result rawResult, String path, float scaleFactor) {
        inactivityTimer.onActivity();
        boolean fromLiveScan = !TextUtils.isEmpty(path);
        if (fromLiveScan) {
            beepManager.playBeepSoundAndVibrate();
        }

        if (linster != null) {
            linster.handleDecode(rawResult, path, scaleFactor);
        } else {
            String resultString = rawResult.getText();
            //FIXME
            if (TextUtils.isEmpty(resultString)) {
                Toast.makeText(getActivity().getApplication(), "扫描二维码失败了", Toast.LENGTH_SHORT).show();
            } else {
                Intent resultIntent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putString(KEY_RESULT, resultString);

                resultIntent.putExtras(bundle);
                getActivity().setResult(Activity.RESULT_OK, resultIntent);
            }
            getActivity().finish();
        }


    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    public void drawViewPause() {
        viewfinderView.drawViewPause();
    }

    public Handler getHandler() {
        return handler;
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, decodeFormats, decodeHints,
                        characterSet, cameraManager);
            }
            decodeOrStoreSavedBitmap(null, null);
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage("初始化摄像头出错了");
        builder.setPositiveButton("知道了", new FinishListener(getActivity()));
        builder.setOnCancelListener(new FinishListener(getActivity()));
        builder.show();
    }

    private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
        // Bitmap isn't used yet -- will be used soon
        if (handler == null) {
            savedResultToShow = result;
        } else {
            if (result != null) {
                savedResultToShow = result;
            }
            if (savedResultToShow != null) {
                Message message = Message.obtain(handler, R.id.decode_succeeded, savedResultToShow);
                handler.sendMessage(message);
            }
            savedResultToShow = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void onPause() {
        super.onPause();
        initPause();
    }

    QRResultLinster linster;

    public void setQRRLinster(QRResultLinster linster) {
        this.linster = linster;
    }

    public static interface QRResultLinster {
        void handleDecode(Result rawResult, String path, float scaleFactor);
    }

}
