package com.like.zxing;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import com.bigkoo.convenientbanner.holder.Holder;

/**
 * Created by Administrator on 2017/8/24 0024.
 */

public class LocalImageHolderView implements Holder<Integer> {
    ImageView imageView;
    Context context;

    public LocalImageHolderView(Context context) {
        this.context = context;
    }

    @Override
    public View createView(Context context) {
        imageView = new ImageView(context);
        return imageView;
    }

    @Override
    public void UpdateUI(Context context, int position, Integer data) {
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        imageView.setImageResource(R.mipmap.ic_launcher);
    }
}
