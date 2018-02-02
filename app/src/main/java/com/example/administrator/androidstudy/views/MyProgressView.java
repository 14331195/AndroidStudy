package com.example.administrator.androidstudy.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Administrator on 2018/2/2.
 */

public class MyProgressView extends View {
    private Paint mPaint;
    private int mProgress;

    public MyProgressView(Context context) {
        super(context);
        init();
    }
    public MyProgressView(Context context, AttributeSet set) {
        super(context, set);
        init();
    }

    private void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setARGB(0xff, 0x66, 0xf6, 0x66);
    }

    public void setProgress(int i) {
        mProgress = i;
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = (int)(getWidth() * (0.01 * mProgress));
        canvas.drawRect(getLeft(), 0, width - getHeight()/2, getBottom(), mPaint);
        canvas.drawCircle(width - getHeight() / 2 + getLeft(), getHeight()/2 , getHeight() / 2, mPaint);
    }
}
