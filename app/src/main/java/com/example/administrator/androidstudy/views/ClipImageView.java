package com.example.administrator.androidstudy.views;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

/**
 * Created by Administrator on 2018/1/10.
 */

public class ClipImageView extends ImageView {
    private Bitmap mBitmap;
    private Matrix mMatrix;
    private Rect mOutRectTop;
    private Rect mOutRectBottom;
    private Rect mOutRectLeft;
    private Rect mOutRectRight;
    private Rect mClipRect;
    private Paint mOutRectPaint;
    private Paint mClipRectPaint;

    private int radius = 800;
    private float MAX_SCALE = 50;
    private float MIN_SCALE;
    private float MIN_EDGE_X;
    private float MAX_EDGE_X;
    private float MIN_EDGE_Y;
    private float MAX_EDGE_Y;

    private boolean isAnimRun = false;

    private PointF mLastMovePoint = new PointF();
    private PointF mLastScalePoint1 = new PointF();
    private PointF mLastScalePoint2 = new PointF();

    public ClipImageView(Context context) {
        super(context);
    }

    public ClipImageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public void setImageBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
        super.setImageBitmap(bitmap);
        init();
    }

    private void init() {
        if (mBitmap == null) {
           return;
        }
        Rect drawRect = new Rect();
        getDrawingRect(drawRect);
        mOutRectTop = new Rect(drawRect.left, drawRect.top, drawRect.right, drawRect.bottom/2-radius/2);
        mOutRectBottom = new Rect(drawRect.left, drawRect.bottom/2+radius/2, drawRect.right, drawRect.bottom);
        mOutRectLeft = new Rect(drawRect.left, mOutRectTop.bottom, drawRect.right/2-radius/2, mOutRectBottom.top);
        mOutRectRight = new Rect(drawRect.right/2+radius/2, mOutRectTop.bottom, drawRect.right, mOutRectBottom.top);
        mOutRectPaint = new Paint();
        mOutRectPaint.setARGB(125, 50, 50, 50);
        mClipRectPaint = new Paint();
        mClipRectPaint.setARGB(10, 255, 255, 255);
        int bmWidth = mBitmap.getWidth();
        int bmHeight = mBitmap.getHeight();
        int left = bmWidth / 2 - radius / 2;
        int top = bmHeight / 2 - radius / 2;
//        mClipRect = new Rect(left, top, left + radius, top + radius);
        mClipRect = new Rect(mOutRectLeft.right, mOutRectTop.bottom, mOutRectRight.left, mOutRectBottom.top);

        float scale = radius / (float) (Math.min(bmHeight, bmWidth));
        if (scale >= 1.0f) {    //图像小于裁剪框的情况，取最短的边计算scale，
            MIN_SCALE = scale;
        } else {                //图像大于裁剪框的情况，取长边计算scale，使得整个图像可以缩放在裁剪框内
            MIN_SCALE = radius / (float) (Math.max(bmHeight, bmWidth));
        }
//        scale = scale > 1.0f ? scale : 1.0f;
        mMatrix = new Matrix();
        mMatrix.setScale(scale, scale);
        float cx = drawRect.width() / 2 - radius / 2;
        float cy = drawRect.height() / 2 - radius / 2;
        mMatrix.postTranslate(cx, cy);
        MIN_EDGE_X = cx;
        MAX_EDGE_X = MIN_EDGE_X + radius;
        MIN_EDGE_Y = cy;
        MAX_EDGE_Y = MIN_EDGE_Y + radius;
        Log.v("AAAA:", "scale:" + scale);
    }

    private final int ACTION_SCALE = 1;
    private final int ACTION_MOVE = 2;
    private int mLastAction;
    private int mActionId;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isAnimRun) {
            return true;
        }
        if (event.getPointerCount() == 1) {
            moveImage(event);
            mLastAction = ACTION_MOVE;
        } else {
            scaleImage(event);
            mLastAction = ACTION_SCALE;
        }
        if (event.getAction() == MotionEvent.ACTION_CANCEL
                || event.getAction() == MotionEvent.ACTION_UP
                || event.getAction() == MotionEvent.ACTION_POINTER_UP) {
            mLastScalePoint1.set(0f, 0f);
            mLastScalePoint2.set(0f, 0f);
        }
        return true;
    }

    private void moveImage(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastMovePoint.set(event.getX(), event.getY());
                mActionId = event.getPointerId(0);
                break;
            case MotionEvent.ACTION_MOVE:
                //若上一次是缩放，松开手指后偶尔会进入这里，此时离上一个触点会比较远，导致图像出现跳跃
                if (mActionId == event.getPointerId(0) && mLastAction != ACTION_SCALE) {
                    mMatrix.postTranslate(event.getX()-mLastMovePoint.x, event.getY()-mLastMovePoint.y);
                    invalidate();
                }
                if (mActionId != event.getPointerId(0)) {
                    mActionId = event.getPointerId(0);
                }
                mLastMovePoint.set(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                adjustToClipWindow();
                break;
        }
    }

    private void scaleImage(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (mLastScalePoint1.x == 0f) {
                    mLastScalePoint1.set(event.getX(0), event.getY(0));
                    mLastScalePoint2.set(event.getX(1), event.getY(1));
                }
                float x1 = event.getX(0);
                float y1 = event.getY(0);
                float x2 = event.getX(1);
                float y2 = event.getY(1);
                float scale = (float) (Math.hypot(x1-x2, y1-y2) /
                        Math.hypot(mLastScalePoint1.x-mLastScalePoint2.x, mLastScalePoint1.y-mLastScalePoint2.y));
                float[] matrixValues = new float[9];
                mMatrix.getValues(matrixValues);
                if (matrixValues[Matrix.MSCALE_X] * scale < MIN_SCALE) {
                    return;
                }
                mMatrix.postScale(scale, scale, (x1 + x2) / 2, (y1 + y2) / 2);
                mLastScalePoint1.set(x1, y1);
                mLastScalePoint2.set(x2, y2);

                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                Log.v("AAA:", "scale cancel");

                adjustToClipWindow();
                break;
        }
    }

    private void adjustToClipWindow() {
        if (mMatrix == null) {
            init();
        }
        final float[] matrixValues = new float[9];
        mMatrix.getValues(matrixValues);
        boolean notMatchWindow = false;
        float trans_x_end = matrixValues[Matrix.MTRANS_X];
        float trans_y_end = matrixValues[Matrix.MTRANS_Y];
        if (matrixValues[Matrix.MTRANS_X] >= MIN_EDGE_X) {
//            notMatchWindow = true;
            trans_x_end = MIN_EDGE_X;
        } else if (matrixValues[Matrix.MTRANS_X] <= MAX_EDGE_X) {
//            notMatchWindow = true;
            trans_x_end = MAX_EDGE_X;
        }
        if (matrixValues[Matrix.MTRANS_Y] >= MIN_EDGE_Y) {
//            notMatchWindow = true;
            trans_y_end = MIN_EDGE_Y;
        } else if (matrixValues[Matrix.MTRANS_Y] <= MAX_EDGE_Y) {
//            notMatchWindow = true;
            trans_y_end = MAX_EDGE_Y;
        }
        if (notMatchWindow) {
            ValueAnimator animatorX = ValueAnimator.ofFloat(matrixValues[Matrix.MTRANS_X], trans_x_end);
            animatorX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    Float x = (Float) animation.getAnimatedValue();
                    matrixValues[Matrix.MTRANS_X] = x;
                    mMatrix.setValues(matrixValues);
                    invalidate();
                }
            });
            ValueAnimator animatorY = ValueAnimator.ofFloat(matrixValues[Matrix.MTRANS_Y], trans_y_end);
            animatorY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    Float y = (Float)animation.getAnimatedValue();
                    matrixValues[Matrix.MTRANS_Y] = y;
                    mMatrix.setValues(matrixValues);
                    invalidate();
                }
            });
            AnimatorSet set = new AnimatorSet();
            set.setDuration(200);
            set.setInterpolator(new DecelerateInterpolator());
            set.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    isAnimRun = true;
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    isAnimRun = false;
                }
                @Override
                public void onAnimationCancel(Animator animation) {}
                @Override
                public void onAnimationRepeat(Animator animation) {}
            });
            set.play(animatorX).with(animatorY);
            set.start();
        }
    }

    public Bitmap clipImage() {
        Bitmap result = Bitmap.createBitmap(mClipRect.width(), mClipRect.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        setDrawingCacheEnabled(false);
        setWillNotCacheDrawing(false);
        setDrawingCacheEnabled(true);
        Bitmap drawingCache = getDrawingCache();
        Rect fitRect = new Rect(0, 0, result.getWidth(), result.getHeight());
//        Rect drawRect = new Rect();
//        getDrawingRect(drawRect);
//        Rect rect = new Rect(mOutRectLeft.right, mOutRectTop.bottom, mOutRectRight.left, mOutRectBottom.top);
        if (drawingCache != null) {
            canvas.drawBitmap(drawingCache, mClipRect, fitRect, null);
        } else {
            result = null;
            Log.v("AAA:", "null");
        }
        Log.v("AAA:", fitRect.width()+"|"+fitRect.height());
        return result;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mMatrix == null) {
            init();
        }
        try {
            canvas.drawBitmap(mBitmap, mMatrix, null);
            canvas.drawRect(mOutRectTop, mOutRectPaint);
            canvas.drawRect(mOutRectBottom, mOutRectPaint);
            canvas.drawRect(mOutRectLeft, mOutRectPaint);
            canvas.drawRect(mOutRectRight, mOutRectPaint);
            canvas.drawRect(mClipRect, mClipRectPaint);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
