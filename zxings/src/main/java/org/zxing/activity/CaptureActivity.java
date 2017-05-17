package org.zxing.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import org.zxing.Utils;
import org.zxing.camera.CameraManager;
import org.zxing.decoding.CaptureActivityHandler;
import org.zxing.decoding.FinishListener;
import org.zxing.decoding.InactivityTimer;
import org.zxing.encoding.QRCodeDecoder;
import org.zxing.view.AbViewfinderView;

import java.io.IOException;
import java.util.Vector;

public abstract class CaptureActivity extends AppCompatActivity implements Callback {

    private CaptureActivityHandler handler;
    private boolean hasSurface;
    private Vector<BarcodeFormat> decodeFormats;
    private String characterSet;
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


    @Override
    protected void onResume() {
        super.onResume();
        SurfaceView surfaceView = getSurfaceView();
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (!hasSurface) {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        } else {
            initCamera(surfaceHolder, getSurfaceView().getWidth(), getSurfaceView().getHeight());
        }
        decodeFormats = null;
        characterSet = null;

    }

    public abstract SurfaceView getSurfaceView();

    @Override
    protected void onPause() {
        super.onPause();
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        hasSurface = false;
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
    public void handleDecode(Result result, Bitmap barcode) {
        vibrate();
    }

    private void initCamera(SurfaceHolder surfaceHolder, int width, int height) {
        try {
            CameraManager.get().openDriver(surfaceHolder, width, height);
        } catch (IOException ioe) {
            showTipAndExit(ioe.getMessage());
            return;
        } catch (RuntimeException e) {
            showTipAndExit(e.getMessage());
            return;
        }
        if (this.handler == null) {
            this.handler = new CaptureActivityHandler(this, this.decodeFormats, this.characterSet);
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
        if (hasSurface) {
            initCamera(holder, width, height);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    public abstract AbViewfinderView getViewfinderView();

    public Handler getHandler() {
        return handler;
    }

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
                    String[] proj = new String[]{"_data"};
                    Cursor cursor = this.getContentResolver().query(data.getData(), proj, (String) null, (String[]) null, (String) null);
                    if (cursor.moveToFirst()) {
                        int column_index = cursor.getColumnIndexOrThrow("_data");
                        photo_path = cursor.getString(column_index);
                        if (photo_path == null) {
                            photo_path = Utils.getPath(this.getApplicationContext(), data.getData());
                        }
                    }
                    cursor.close();
                    new AsyncTasks(this, this.photo_path).execute();
                    break;

                default:
                    break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    static class AsyncTasks extends AsyncTask<String, Void, Result> {
        CaptureActivity activity;
        String path;

        public AsyncTasks(CaptureActivity captureActivity, String path) {
            activity = captureActivity;
            this.path = path;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected Result doInBackground(String... params) {
            return QRCodeDecoder.syncDecodeQRCode(path);
        }


        @Override
        protected void onPostExecute(Result result) {
            if (result == null) {
                Toast.makeText(activity.getApplicationContext(), "未发现二维码", Toast.LENGTH_SHORT).show();
            } else {
                activity.handleDecode(result, QRCodeDecoder.getDecodeAbleBitmap(this.path));
            }
        }
    }

    protected void photo() {
        Intent innerIntent = new Intent();
        if (Build.VERSION.SDK_INT < 19) {
            innerIntent.setAction("android.intent.action.GET_CONTENT");
        } else {
            innerIntent.setAction("android.intent.action.OPEN_DOCUMENT");
        }

        innerIntent.setType("image/*");
        Intent wrapperIntent = Intent.createChooser(innerIntent, "选择二维码图片");
        this.startActivityForResult(wrapperIntent, 234);
    }

    static class Listener implements OnCompletionListener {
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.seekTo(0);
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