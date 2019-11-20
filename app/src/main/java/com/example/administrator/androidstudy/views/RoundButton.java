package com.example.administrator.androidstudy.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;

/**
 * Created by ljm on 2018/5/16.
 */
public class RoundButton extends Button {
    private Paint mPaint;
    private int mLastWidth;

    public RoundButton(Context context) {
        super(context);
    }

    public RoundButton(Context context, AttributeSet set) {
        super(context, set);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.BLACK);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        mLastWidth = width;
        int size = Math.min(width, height);
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        super.onMeasure(MeasureSpec.makeMeasureSpec(size, widthSpecMode),
                MeasureSpec.makeMeasureSpec(size, heightSpecMode));
//        setMeasuredDimension(size, size);
    }

//    @Override
//    public void onDraw(Canvas canvas) {
////        if (getMeasuredWidth() != mLastWidth) {
////            canvas.translate(-getMeasuredWidth()/2, 0);
//////            canvas.translate(getMeasuredWidth() / 2, 0);
////        }
//        super.onDraw(canvas);
//    }
}
