package com.example.administrator.androidstudy.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.support.v4.util.Pair;
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
//        int width = (int)(getWidth() * (0.01 * mProgress));
//        canvas.drawRect(getLeft(), 0, width - getHeight()/2, getBottom(), mPaint);
//        canvas.drawCircle(width - getHeight() / 2 + getLeft(), getHeight()/2 , getHeight() / 2, mPaint);



        Path path = new Path();
        path.moveTo(0, getHeight()/2 - 10);
        path.lineTo(0, getHeight()/2 + 10);
        path.lineTo(getWidth(), getHeight() - 10);
        path.lineTo(getWidth(), 10);
        path.close();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.GRAY);
//        canvas.drawPath(path, paint);

        int sc = canvas.saveLayer(0, 0, canvas.getWidth(), canvas.getHeight(), null, Canvas.ALL_SAVE_FLAG);
        canvas.drawPath(path, paint);

        Rect rect = new Rect(0, 0, getWidth()/2, getHeight());
        Paint paint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint1.setColor(Color.RED);
        paint1.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawRect(rect, paint1);
        paint1.setXfermode(null);

        canvas.restoreToCount(sc);
    }
}
