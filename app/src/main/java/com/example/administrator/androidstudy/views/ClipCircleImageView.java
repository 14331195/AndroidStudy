package com.example.administrator.androidstudy.views;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

/**
 * Created by Administrator on 2018/1/10.
 */

public class ClipCircleImageView extends ImageView {
    private Bitmap mBitmap;
    private Matrix mMatrix;
    private Rect mOutRect;
    private Rect mClipRect;
    private Paint mOutRectPaint;            //外部区域填充画笔
    private Paint mClipRectPaint;           //裁剪区域内容填充画笔
    private Paint mClipRectStrokePaint;     //裁剪区域边界画笔

    private int radius = 800;
    private float MIN_SCALE;
    private float MIN_EDGE_X;
    private float MAX_EDGE_X;
    private float MIN_EDGE_Y;
    private float MAX_EDGE_Y;

    private boolean isAnimRun = false;

    private PointF mLastMovePoint = new PointF();
    private PointF mLastScalePoint1 = new PointF();
    private PointF mLastScalePoint2 = new PointF();

    public ClipCircleImageView(Context context) {
        super(context);
    }

    public ClipCircleImageView(Context context, AttributeSet attributeSet) {
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
        mOutRect = new Rect(drawRect.left, drawRect.top, drawRect.right, drawRect.bottom);
        mOutRectPaint = new Paint();
        mOutRectPaint.setARGB(125, 0, 0, 0);
        mClipRectPaint = new Paint();
        mClipRectPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mClipRectStrokePaint = new Paint();
        mClipRectStrokePaint.setStrokeWidth(3);
        mClipRectStrokePaint.setStyle(Paint.Style.STROKE);
        mClipRectStrokePaint.setColor(Color.WHITE);
        int bmWidth = mBitmap.getWidth();
        int bmHeight = mBitmap.getHeight();
//        mClipRect = new Rect(mOutRectLeft.right, mOutRectTop.bottom, mOutRectRight.left, mOutRectBottom.top);

        MIN_SCALE = radius / (float) (Math.min(bmHeight, bmWidth));
        mMatrix = new Matrix();
        mMatrix.setScale(MIN_SCALE, MIN_SCALE);
        float cx = drawRect.width() / 2 - radius / 2;
        float cy = drawRect.height() / 2 - radius / 2;
        mMatrix.postTranslate(cx, cy);
        MIN_EDGE_X = cx;
        MAX_EDGE_X = MIN_EDGE_X + radius;
        MIN_EDGE_Y = cy;
        MAX_EDGE_Y = MIN_EDGE_Y + radius;
        Log.v("AAAA:", "scale:" + MIN_SCALE);
    }

    private final int ACTION_SCALE = 1;
    private final int ACTION_MOVE = 2;
    private int mLastAction;
    private int mActionId;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isAnimRun || mMatrix == null) {
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
                if (matrixValues[Matrix.MSCALE_X] * scale <= MIN_SCALE) {
                    scale = MIN_SCALE / matrixValues[Matrix.MSCALE_X];
                }
                mMatrix.postScale(scale, scale, (x1 + x2) / 2, (y1 + y2) / 2);
                mLastScalePoint1.set(x1, y1);
                mLastScalePoint2.set(x2, y2);

                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
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
        float currX = mBitmap.getWidth() * matrixValues[Matrix.MSCALE_X] + matrixValues[Matrix.MTRANS_X];
        float currY = mBitmap.getHeight() * matrixValues[Matrix.MSCALE_Y] + matrixValues[Matrix.MTRANS_Y];
        if (matrixValues[Matrix.MTRANS_X] > MIN_EDGE_X) {
            notMatchWindow = true;
            trans_x_end = MIN_EDGE_X;
        } else if (currX < MAX_EDGE_X) {
            notMatchWindow = true;
            trans_x_end = MAX_EDGE_X - currX + matrixValues[Matrix.MTRANS_X];
        }
        if (matrixValues[Matrix.MTRANS_Y] > MIN_EDGE_Y) {
            notMatchWindow = true;
            trans_y_end = MIN_EDGE_Y;
        } else if (currY < MAX_EDGE_Y) {
            notMatchWindow = true;
            trans_y_end = MAX_EDGE_Y - currY + matrixValues[Matrix.MTRANS_Y];
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
        Bitmap result = Bitmap.createBitmap(radius, radius, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        setDrawingCacheEnabled(false);
        setWillNotCacheDrawing(false);
        setDrawingCacheEnabled(true);
        Bitmap drawingCache = getDrawingCache();
        if (drawingCache != null) {
            Rect desRect = new Rect(0, 0, result.getWidth(), result.getHeight());
            Rect srcRect = new Rect((int)MIN_EDGE_X , (int)MIN_EDGE_Y, (int)MIN_EDGE_X + radius, (int)MIN_EDGE_Y + radius);
            canvas.drawBitmap(drawingCache, srcRect, desRect, null);

            Bitmap tmp = result;
            result = Bitmap.createBitmap(radius, radius, Bitmap.Config.ARGB_8888);
            canvas = new Canvas(result);
            Path path = new Path();
            path.addCircle(radius/2 , radius/2, radius / 2 - 3, Path.Direction.CW);
            canvas.clipPath(path);
            canvas.drawBitmap(tmp, 0, 0, new Paint());
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mMatrix == null) {
            init();
        }
        try {
            canvas.drawBitmap(mBitmap, mMatrix, null);
            int layerId = canvas.saveLayer(0, 0, canvas.getWidth(), canvas.getHeight(), null, Canvas.ALL_SAVE_FLAG);
            canvas.drawRect(mOutRect, mOutRectPaint);
            canvas.drawCircle(mOutRect.width()/2, mOutRect.height() / 2, radius / 2, mClipRectPaint);
            canvas.restoreToCount(layerId);
            canvas.drawCircle(mOutRect.width()/2, mOutRect.height() / 2, radius / 2, mClipRectStrokePaint);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
