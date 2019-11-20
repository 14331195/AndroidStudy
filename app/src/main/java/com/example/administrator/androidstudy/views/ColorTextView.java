package com.example.administrator.androidstudy.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

/**
 * Created by ljm on 2018/5/18.
 * 模仿QQ答题活动下拉的背景样式
 */
public class ColorTextView extends TextView {
    private Paint mPaint;
    private Path mPath;
    private int mVisibleHeight;

    public ColorTextView(Context context) {
        super(context);
        init();
    }

    public ColorTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    private void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.FILL);
        mPath = new Path();
    }

    public void drawColorBg(int scrollY) {
        mVisibleHeight = Math.abs(scrollY);
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        mPath.reset();      //注意这里需要reset，不然会保留上一次绘制的画面
        mPath.moveTo(0, getHeight() - mVisibleHeight);
        mPath.quadTo(width / 2, mVisibleHeight * 2 / 3 + getHeight() - mVisibleHeight,
                width, getHeight() - mVisibleHeight);
        mPath.close();
        canvas.drawPath(mPath, mPaint);
        super.onDraw(canvas);
    }
}
