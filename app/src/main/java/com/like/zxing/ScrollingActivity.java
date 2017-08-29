package com.like.zxing;

import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.bigkoo.convenientbanner.ConvenientBanner;
import com.bigkoo.convenientbanner.holder.CBViewHolderCreator;

import java.util.ArrayList;

public class ScrollingActivity extends AppCompatActivity implements AppBarLayout.OnOffsetChangedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        ConvenientBanner banner = (ConvenientBanner) findViewById(R.id.banner);
        ArrayList list = new ArrayList();
        list.add(1);
        list.add(1);
        list.add(1);
        list.add(1);
        list.add(1);
        banner.setPages(new CBViewHolderCreator() {

            @Override
            public Object createHolder() {
                return new LocalImageHolderView(ScrollingActivity.this
                );
            }
        },list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.app_bar);
        appBarLayout.addOnOffsetChangedListener(this);
        setSupportActionBar(toolbar);
        mTabNoText = findViewById(R.id.tl_collapse);
        mTitleLayout = findViewById(R.id.tl_expand);
        mTabText = findViewById(R.id.v_pay);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        final VerticalSwipeRefreshLayout refreshLayout = (VerticalSwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2000);
                            refreshLayout.setRefreshing(false);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    private View mTabNoText;

    private View mTitleLayout;
    private View mTabText;

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        int offset = Math.abs(verticalOffset);
        int total = appBarLayout.getTotalScrollRange();
        int alphaIn = 255 * offset / total;
        int alphaOut = 255 - 255 * offset / total;

        if (offset <= (total / 2)) {
            mTitleLayout.setVisibility((offset > total / 4) ? View.GONE : View.VISIBLE);
            mTabNoText.setVisibility((offset > total / 4) ? View.VISIBLE : View.GONE);

        } else {

        }
        mTitleLayout.setAlpha(alphaOut / 255.0f);
        mTabNoText.setAlpha(alphaIn / 255.0f);
        mTabText.setAlpha((total - offset) / (total * 1.0f));
    }
}
