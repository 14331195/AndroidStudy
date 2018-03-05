package com.example.administrator.androidstudy.views;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.example.administrator.androidstudy.R;
import com.example.administrator.androidstudy.utils.Utils;

/**
 * Created by Administrator on 2018/3/5.
 */

public class HeaderLayout extends FrameLayout {
    private RecyclerView mRecyclerView;
    private HeaderPointView mHeaderPointView;

    private int mPointHeight = Utils.dip2pix(60);
    private int mHeaderHeight = Utils.dip2pix(120);

    public HeaderLayout(Context context){
        this(context, null, 0);
    }

    public HeaderLayout(Context context, AttributeSet set) {
        this(context, set, 0);
    }

    public HeaderLayout(Context context, AttributeSet set, int defStyle) {
        super(context, set, defStyle);
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_header, null);
        addView(view);
        mRecyclerView = findViewById(R.id.recycler_view);
        mHeaderPointView = findViewById(R.id.header_point);
    }

    public void onPullDown(int offset) {
        float percentage = Math.abs(offset) / (float)mPointHeight;
        if (percentage <= 1.0f) {
            mHeaderPointView.setVisibility(VISIBLE);
            mHeaderPointView.setPercentage(percentage);
            mRecyclerView.setTranslationY(-mPointHeight);
        } else {
            int extra = offset - mPointHeight;
            percentage = extra / (float)(mHeaderHeight - mPointHeight);
            mHeaderPointView.setPercentage(1.0f);
            mHeaderPointView.setTranslationY(mHeaderPointView.getHeight() / 2 * percentage);
            mHeaderPointView.setAlpha(Math.max(1 - percentage, 0));
            mRecyclerView.setTranslationY(-mPointHeight * (1 - percentage));
        }
    }

    public int getPointHeight() {
        return mPointHeight;
    }

    public int getHeaderHeight() {
        return mHeaderHeight;
    }
}
