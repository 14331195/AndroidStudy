package com.example.administrator.androidstudy.views;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.example.administrator.androidstudy.R;
import com.example.administrator.androidstudy.utils.Utils;

/**
 * Created by Administrator on 2018/3/5.
 */

public class HeaderLayout extends FrameLayout {
    private RecyclerView mRecyclerView;
    private HeaderPointView mHeaderPointView;

    private int mPointHeightDip = 160;
    private int mPointHeight = mPointHeightDip; //Utils.dip2pix(mPointHeightDip);
    private int mHeaderHeight = 320;//Utils.dip2pix(320);
    private int mCurrHeaderHeight = mHeaderHeight;
    private boolean isShowing;      //头部布局是否已全部展示出来

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
        isShowing = false;
        View view = LayoutInflater.from(context).inflate(R.layout.layout_header, null);
        addView(view);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mHeaderPointView = (HeaderPointView) findViewById(R.id.header_point);

        post(new Runnable() {
            @Override
            public void run() {
                mHeaderHeight = getMeasuredHeight();
                mPointHeight = mHeaderHeight * 2 / 3;
                mRecyclerView.setTranslationY(-mPointHeight);
                ViewGroup.LayoutParams layoutParams = mHeaderPointView.getLayoutParams();
                layoutParams.height = mPointHeight;
                mHeaderPointView.setLayoutParams(layoutParams);
            }
        });
    }

    public boolean canScroll(int scrollY) {
        return !isShowing && scrollY < mHeaderHeight;
    }

    public void onPullDown(int offset) {
        if (mHeaderPointView.getVisibility() == GONE) return;
//        offset = Utils.dip2pix(offset);
        float percentage = Math.abs(offset) / (float)mPointHeight;
//        Log.v("AAAA:", "precentage:"+percentage + ",offset:"+offset);
        if (percentage <= 1.0f) {
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

    public void reset() {
//        mHeaderPointView.setPercentage(1.0f);
        mHeaderPointView.setTranslationY(0);
        mHeaderPointView.setAlpha(1);
        mHeaderPointView.setVisibility(VISIBLE);
    }

    public void hidePointView() {
        mHeaderPointView.setVisibility(GONE);
    }

    public void showPointView() {
        mHeaderPointView.setVisibility(VISIBLE);
    }

    public boolean contains(int x, int y) {
        int[] loc = new int[2];
        getLocationOnScreen(loc);
        return (x >= loc[0] && x <= loc[0]+getWidth() && y >= loc[1] && y <= loc[1]+getHeight());
    }

    public int getPointHeight() {
        return mPointHeight;
    }

    public int getHeaderHeight() {
        return mHeaderHeight;
    }

    public boolean isShowing() {return isShowing;}

    public void postScaleY(float scrollY){
//        isShowing = true;
        mCurrHeaderHeight += scrollY;
        ViewGroup.LayoutParams params = getLayoutParams();
        params.height = mCurrHeaderHeight;
        setLayoutParams(params);
        if (mCurrHeaderHeight > mHeaderHeight) {
            isShowing = true;
        } else {
            isShowing = false;
        }
    }

    public void onCancel() {
        if (mCurrHeaderHeight <= mHeaderHeight) return;
        ValueAnimator animator = ValueAnimator.ofInt(mCurrHeaderHeight, mHeaderHeight);
        animator.addUpdateListener((animation) -> {
            int height = (Integer) animation.getAnimatedValue();
            ViewGroup.LayoutParams params = getLayoutParams();
            params.height = height;
            setLayoutParams(params);
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrHeaderHeight = mHeaderHeight;
                isShowing = false;
            }
            @Override
            public void onAnimationCancel(Animator animation) {
            }
            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animator.setDuration(300);
        animator.start();
    }


//    @Override
//    public boolean onTouchEvent(MotionEvent event){
//        switch (event.getAction()) {
////            case
//        }
//    }
}