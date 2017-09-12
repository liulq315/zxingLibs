package com.like.zxing;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import org.loqs.zxing.activity.QRCodeFragment;

public class CaptureActivity1 extends AppCompatActivity {
    Button mLight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera);
        mLight = (Button) findViewById(R.id.open);
        getFragmentManager().beginTransaction().add(R.id.content, new QRCodeFragment(), "QRCodeFragment").commit();
    }

    public void ssButtonss(View view) {
        QRCodeFragment fragment = (QRCodeFragment) getFragmentManager().findFragmentByTag("QRCodeFragment");
        fragment.photo();
    }

    public void ssopen(View view) {
        QRCodeFragment fragment = (QRCodeFragment) getFragmentManager().findFragmentByTag("QRCodeFragment");
        mLight.setText(fragment.lightOnAndOff() ? "关闭灯光" : "打开灯光");
    }
}
