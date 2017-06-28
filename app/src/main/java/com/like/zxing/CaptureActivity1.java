package com.like.zxing;

import android.content.Intent;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.google.zxing.Result;

import org.zxing.activity.CaptureActivity;
import org.zxing.view.AbViewfinderView;
import org.zxing.view.ViewfinderView;

public class CaptureActivity1 extends CaptureActivity {


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera);
    }

    public void ssButtonss(View view) {
        photo();
    }

    public void ssopen(View view) {
        lightOnAndOff();
    }

    @Override
    public AbViewfinderView getViewfinderView() {
        return (ViewfinderView) findViewById(R.id.viewfinder_view);
    }

    @Override
    public SurfaceView getSurfaceView() {
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
        return surfaceView;
    }


    @Override
    public void handleDecode(Result result, String barcode) {
        super.handleDecode(result, barcode);
        String resultString = result.getText();
        //FIXME
        if (resultString.equals("")) {
            Toast.makeText(getApplicationContext(), "Scan failed!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), resultString + "Scan!" + (barcode != null), Toast.LENGTH_SHORT).show();
//			System.out.println("Result:"+resultString);
            Intent resultIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putString("path", barcode);
            bundle.putString("result", resultString);
            resultIntent.putExtras(bundle);
            this.setResult(RESULT_OK, resultIntent);
        }
        finish();
    }
}