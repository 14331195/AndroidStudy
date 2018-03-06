package com.example.administrator.androidstudy.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by Administrator on 2018/3/5.
 */

public class HeaderPointView extends View {
    private float mPercentage;
    private int mMaxRadius = 20;
    private int mMargin = 100;
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
        mPaint.setColor(Color.RED);
    }

    public void setPercentage(float percentage) {
        mPercentage = percentage;
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = (int)(getHeight() * mPercentage);
        float radius = mMaxRadius * (mPercentage * 2);
//        Log.v("AAAA:", "mPercentage:"+mPercentage + ",height:"+height + "|" + (getHeight()- height / 2));
        if (mPercentage <= 0.5f) {
            /* 中间的圆点从0变到mMaxRadius， 然后再从mMaxRadius变到mMaxRadius/2 */
            canvas.drawCircle(width / 2, getHeight()- height / 2, radius, mPaint);
        } else {
            radius = (1.0f - (mPercentage - 0.5f)) * mMaxRadius;
            canvas.drawCircle(width / 2, getHeight()- height / 2, radius, mPaint);
            canvas.drawCircle(width / 2 - mMargin * (mPercentage - 0.5f) - 10, getHeight()- height / 2, mMaxRadius / 2, mPaint);
            canvas.drawCircle(width / 2 + mMargin * (mPercentage - 0.5f) + 10, getHeight()- height / 2, mMaxRadius / 2, mPaint);
        }
    }
}
