package com.like.zxing;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.loqs.zxing.encoding.EncodingHandler;


public class MainActivity extends AppCompatActivity {

    private TextView resultTextView;
    private ImageView show;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resultTextView = (TextView) findViewById(R.id.textView);
        show = (ImageView) findViewById(R.id.show);
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(MainActivity.this, CaptureActivity1.class), 0);
            }
        });
        findViewById(R.id.EncodingHandler).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText text = (EditText) findViewById(R.id.editText);
                try {
                    show.setImageBitmap(EncodingHandler.createQRCode(text.getText().toString(), dip2px(30), dip2px(30), BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public int px2dip(float pxValue) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * dp转成px
     *
     * @param dipValue
     * @return
     */
    public int dip2px(float dipValue) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //处理扫描结果（在界面上显示）
        if (resultCode == RESULT_OK) {
            Bundle bundle = data.getExtras();
            String path = bundle.getString("path");
            if (!TextUtils.isEmpty(path))
                show.setImageBitmap(BitmapFactory.decodeFile(path));

            String scanResult = bundle.getString("result");
            resultTextView.setText(scanResult);
        }
    }
}
