package org.zxing.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.Toast;

import com.google.zxing.Result;

import org.zxing.Utils;
import org.zxing.camera.CameraManager;
import org.zxing.decoding.CaptureActivityHandler;
import org.zxing.decoding.FinishListener;
import org.zxing.decoding.InactivityTimer;
import org.zxing.encoding.QRCodeDecoder;
import org.zxing.view.AbViewfinderView;

import java.io.IOException;

public abstract class CaptureActivity extends AppCompatActivity implements Callback {

    private boolean hasSurface;
    protected InactivityTimer inactivityTimer;
    private static final long VIBRATE_DURATION = 200L;
    private boolean mLightOn = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CameraManager.init(getApplication());
        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);
    }

    boolean askedPermission = false;

    @TargetApi(23)
    private void openCameraWithPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startInit();
        } else if (!askedPermission) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    0x456);
            askedPermission = true;
        } else {
            showTipAndExit("");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == 0x456) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initCamera(surfaceView.getHolder(), getSurfaceView().getWidth(), getSurfaceView().getHeight());
            } else {
                showTipAndExit("");
            }
            return;
        } else if (requestCode == 0x457) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                photo();
            } else {
                Toast.makeText(getApplicationContext(), "获取文件读写权限失败", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    SurfaceView surfaceView;

    private void startInit() {
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        Log.e("onResume", "hasSurface = " + hasSurface);
        if (!hasSurface) {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        } else {
            initCamera(surfaceHolder, getSurfaceView().getWidth(), getSurfaceView().getHeight());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        surfaceView = getSurfaceView();
        if (Build.VERSION.SDK_INT >= 23) {
            openCameraWithPermission();
        } else {
            startInit();
        }


    }

    public abstract SurfaceView getSurfaceView();

    @Override
    protected void onPause() {
        super.onPause();
        if (CaptureActivityHandler.getHandler() != null) {
            CaptureActivityHandler.getHandler().quitSynchronously();
        }
        CameraManager.get().closeDriver();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }

    /**
     * Handler scan result
     *
     * @param result
     * @param barcode
     */
    public void handleDecode(Result result, String barcode) {
        vibrate();
        Log.e("End----->", "end - time = " + System.currentTimeMillis());
    }

    private void initCamera(SurfaceHolder surfaceHolder, int width, int height) {
        try {
            CameraManager.get().openDriver(this, surfaceHolder, width, height);
        } catch (IOException ioe) {
            showTipAndExit(ioe.getMessage());
            return;
        } catch (RuntimeException e) {
            showTipAndExit(e.getMessage());
            return;
        }
    }

    protected void showTipAndExit(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("提示");
        builder.setMessage("无法获取摄像头数据，\n请检查是否已经开启摄像头权限。").setPositiveButton("确定", new FinishListener(this));
        builder.create().show();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        Log.e("surfaceChanged", " hasSurface =" + hasSurface);
        if (hasSurface) {
            initCamera(holder, width, height);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e("surfaceCreated", " hasSurface =" + hasSurface);
        if (!hasSurface) {
            hasSurface = true;
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
        Log.e("surfaceDestroyed", " hasSurface =" + hasSurface);
        getSurfaceView().getHolder().removeCallback(this);
    }

    public abstract AbViewfinderView getViewfinderView();

    public void drawViewfinder() {
        getViewfinderView().drawViewfinder();
    }


    protected void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(VIBRATE_DURATION);
    }

    String photo_path = null;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 234:

                    photo_path = getSelectMediaPath(data);
                    if (TextUtils.isEmpty(photo_path)) {
                        photo_path = Utils.getPath(this.getApplicationContext(), data.getData());
                    }
                    new AsyncTasks(this).execute(this.photo_path);
                    break;

                default:
                    break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private String getSelectMediaPath(Intent data) {
        String path = "";
        Uri uri = data.getData(); // 获取别选中图片的uri
        if (uri.toString().contains("file:///")) {
            path = uri.toString().replace("file:///", "");
        } else {
            String[] filePathColumn = new String[]{MediaStore.Images.Media.DATA}; // 获取图库图片路径
            Cursor cursor = getContentResolver().query(uri,
                    filePathColumn, null, null, null); // 查询选中图片路径
            if (cursor != null) {
                cursor.moveToFirst();
                path = cursor.getString(cursor
                        .getColumnIndex(filePathColumn[0]));
                cursor.close();
            }
        }
        return path;
    }

    static class AsyncTasks extends AsyncTask<String, Void, Result> {
        CaptureActivity activity;
        String path;

        public AsyncTasks(CaptureActivity captureActivity) {
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
                Toast.makeText(activity.getApplicationContext(), "未发现二维码", Toast.LENGTH_SHORT).show();
            } else {
                activity.handleDecode(result, (this.path));
            }
        }
    }

    protected void photo() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            Intent innerIntent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            innerIntent.setType("image/*");
            Intent wrapperIntent = Intent.createChooser(innerIntent, "选择二维码图片");
            this.startActivityForResult(wrapperIntent, 234);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    0x457);
        }
    }

    protected void lightOnAndOff() {
        if (this.mLightOn)
            CameraManager.get().offLight();
        else
            CameraManager.get().openLight();
        mLightOn = !mLightOn;
    }
}