package com.example.administrator.androidstudy.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Administrator on 2018/3/5.
 */

public class HeaderPointView extends View {
    private float mPercentage;
    private int mMaxRadius = 10;
    private int mMargin = 20;
    private Paint mPaint;

    public HeaderPointView(Context context) {
        super(context);
        init();
    }

    public HeaderPointView(Context context, AttributeSet set) {
        super(context, set);
        init();
    }

    private void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.GRAY);
    }

    public void setPercentage(float percentage) {
        mPercentage = percentage;
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (mPercentage <= 0.5f) {
            /* 中间的圆点从0变到mMaxRadius， 然后再从mMaxRadius变到mMaxRadius/2 */
            canvas.drawCircle(width / 2, height / 2, mMaxRadius * (mPercentage * 2), mPaint);
        } else {
            canvas.drawCircle(width / 2, height / 2, (1.5f - mPercentage) * mMaxRadius, mPaint);
            canvas.drawCircle(width / 2 - mMargin * (mPercentage - 0.5f), height / 2, mMaxRadius / 2, mPaint);
            canvas.drawCircle(width / 2 + mMargin * (mPercentage - 0.5f), height / 2, mMaxRadius / 2, mPaint);
        }
    }
}
