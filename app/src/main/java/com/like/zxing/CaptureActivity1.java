package com.like.zxing;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import org.lqos.zxing.activity.QRCodeFragment;

public class CaptureActivity1 extends AppCompatActivity {
    Button mLight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera);
        mLight = (Button) findViewById(R.id.open);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.CAMERA},
                    0x457);
        } else {
            getFragmentManager().beginTransaction().add(R.id.content, new QRCodeFragment(), "QRCodeFragment").commit();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0x457) {
            getFragmentManager().beginTransaction().add(R.id.content, new QRCodeFragment(), "QRCodeFragment").commit();
        }
    }

    public void ssButtonss(View view) {
        QRCodeFragment fragment = (QRCodeFragment) getFragmentManager().findFragmentByTag("QRCodeFragment");
        fragment.getHandler().sendEmptyMessage(R.id.restart_pause);
    }

    public void ssopen(View view) {
        QRCodeFragment fragment = (QRCodeFragment) getFragmentManager().findFragmentByTag("QRCodeFragment");
        mLight.setText(fragment.lightOnAndOff() ? "关闭灯光" : "打开灯光");
        fragment.getHandler().sendEmptyMessage(R.id.restart_restart);
    }
}
