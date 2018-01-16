package com.example.administrator.androidstudy.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by Administrator on 2018/1/15.
 */

public class PhotoWallView extends View {
    private final int mSpacing = 5;
    private int mWidth;
    private int mHeight;
    private Rect[] mSrcRects = new Rect[5];
    private Rect[] mSmallDesRects = new Rect[4];
    private Rect mBigDesRect;
    private Bitmap mInitBitmap;

    public PhotoWallView(Context context) {
        super(context);
    }

    public PhotoWallView(Context context, AttributeSet set) {
        super(context, set);
    }

    public void setInitBitmap(Bitmap bitmap) {
        mInitBitmap = bitmap;
    }

    private void init() {
        if (mInitBitmap == null) {
            return;
        }

        mSrcRects[0] = new Rect(0, 0, mInitBitmap.getWidth(), mInitBitmap.getHeight());
        int width = mWidth / 4;
        int left = mWidth / 2;
        Log.v("AAAA:", ""+ getPaddingLeft() + "," + getPaddingBottom());

        mBigDesRect = new Rect(0, 0, left, mHeight);
        mSmallDesRects[0] = new Rect(left, 0, left + width, width);
        mSmallDesRects[1] = new Rect(left + width + mSpacing, 0, mSmallDesRects[0].right + mSpacing + width, width);
        mSmallDesRects[2] = new Rect(left, mSmallDesRects[0].bottom + mSpacing, left + width, mSmallDesRects[0].bottom + mSpacing + width);
        mSmallDesRects[3] = new Rect(mSmallDesRects[0].right + mSpacing,         mSmallDesRects[0].bottom + mSpacing,
                                     mSmallDesRects[0].right + mSpacing + width, mSmallDesRects[0].bottom + mSpacing + width);

    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        mWidth = getWidth();
        mHeight = getHeight();
        init();
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mInitBitmap == null) {
            return;
        }
        try {
            canvas.drawBitmap(mInitBitmap, mSrcRects[0], mBigDesRect, null);
            canvas.drawBitmap(mInitBitmap, mSrcRects[0], mSmallDesRects[0], null);
            canvas.drawBitmap(mInitBitmap, mSrcRects[0], mSmallDesRects[1], null);
            canvas.drawBitmap(mInitBitmap, mSrcRects[0], mSmallDesRects[2], null);
            canvas.drawBitmap(mInitBitmap, mSrcRects[0], mSmallDesRects[3], null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
