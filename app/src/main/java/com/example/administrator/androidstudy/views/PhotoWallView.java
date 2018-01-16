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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.example.administrator.androidstudy.R;

/**
 * Created by Administrator on 2018/1/15.
 */

public class PhotoWallView extends View {
    private static final int SELECT_PHOTO_0 = 0;
    private static final int SELECT_PHOTO_1 = 1;
    private static final int SELECT_PHOTO_2 = 2;
    private static final int SELECT_PHOTO_3 = 3;
    private static final int SELECT_PHOTO_4 = 4;
    private final int mSpacing = 5;
    private int mWidth;
    private int mHeight;
    private int mSelectedPos = -1;
    private int mIntersectRectPos = -1;
    private int mDownX;
    private int mDownY;
    private long mTimeStamp;
    private Rect[] mSrcRects = new Rect[5];
    private Rect[] mDesRects = new Rect[5];
    private Bitmap[] mDesBitmaps = new Bitmap[5];
    private Bitmap mInitBitmap;
    private Bitmap mBitmap;
    private Paint mPaint;

    private WindowManager.LayoutParams mLayoutParams;
    private WindowManager mWindowManager;
    private ImageView mMoveImageView;
    private boolean mIsAnimRunning = false;

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
        for (int i = 0; i < mDesBitmaps.length; ++i) {
            mDesBitmaps[i] = mInitBitmap;
            mSrcRects[i] = new Rect(0, 0, mInitBitmap.getWidth(), mInitBitmap.getHeight());
        }
        mPaint = new Paint();
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.XOR));
        mPaint.setColor(Color.parseColor("#606060"));
        mPaint.setStyle(Paint.Style.FILL);
        int width = (mWidth - mSpacing * 8) / 4;
        int left = mWidth / 2;
//        mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ss0)
//        Log.v("AAAA:", ""+ getPaddingLeft() + "," + getPaddingBottom());

        mDesRects[0] = new Rect(mSpacing, mSpacing, mSpacing + left, mHeight - mSpacing);
        left = mDesRects[0].right + mSpacing;
        mDesRects[1] = new Rect(left, mSpacing, left + width, width + mSpacing);
        mDesRects[2] = new Rect(left + width + mSpacing, mSpacing, mDesRects[1].right + mSpacing + width, width + mSpacing);
        mDesRects[3] = new Rect(left, mDesRects[1].bottom + mSpacing, left + width, mDesRects[1].bottom + mSpacing + width);
        mDesRects[4] = new Rect(mDesRects[1].right + mSpacing,         mDesRects[1].bottom + mSpacing,
                mDesRects[1].right + mSpacing + width, mDesRects[1].bottom + mSpacing + width);

    }

    private void createMoveView() {
        if (mSelectedPos == -1) {
            return;
        }
        if (mWindowManager == null) {
            mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            mLayoutParams = new WindowManager.LayoutParams();
            mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            mLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
            mLayoutParams.width = mWidth / 4;
            mLayoutParams.height = mWidth / 4;
        }
        mLayoutParams.x = mDownX - mLayoutParams.width / 2;
        mLayoutParams.y = mDownY - mLayoutParams.height / 2;
        mMoveImageView = new ImageView(getContext());
        mMoveImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mMoveImageView.setImageBitmap(mDesBitmaps[mSelectedPos]);
        mMoveImageView.setBackgroundColor(Color.WHITE);
        mWindowManager.addView(mMoveImageView, mLayoutParams);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (mWidth != 0) {
            return;
        }
        mWidth = getWidth();
        mHeight = getHeight();
        init();
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = (int)event.getX();
                mDownY = (int)event.getY();
                mTimeStamp = System.currentTimeMillis();
//                Log.v("AAAA:", "mDownX1:"+event.getX()+",mDownY2:"+event.getY());
                checkSelectedPos();
                createMoveView();
                invalidate();
                return true;        //这里要返回true，才能接收到后续的move事件
//                break;
            case MotionEvent.ACTION_MOVE:
//                Log.v("AAAA:", "move");
                int x = (int)event.getX();
                int y = (int)event.getY();
                int[] loc = new int[2];
                this.getLocationOnScreen(loc);
                mLayoutParams.x = x - mLayoutParams.width / 2;
                mLayoutParams.y = y - mLayoutParams.height / 2;
                mWindowManager.updateViewLayout(mMoveImageView, mLayoutParams);
                if (!mIsAnimRunning) {
                    checkIntersectRect();
                    adjustAllRects();
                }
//                Log.v("AAAA:", "x:"+x+",y:"+y);
                break;
            case MotionEvent.ACTION_UP:
                if (System.currentTimeMillis() - mTimeStamp < 100) {
                    if (mSelectedPos == -1) {
                        checkSelectedPos();
                    }
                    if (mListener != null) {
                        mListener.onItemClick(mSelectedPos);
                    }
                }
                mSelectedPos = -1;
                mWindowManager.removeView(mMoveImageView);
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
//                Log.v("AAAA:", "cancel");
//                mSelectedPos = -1;
//                mWindowManager.removeView(mMoveImageView);
//                invalidate();
//                if (event.getAction() == MotionEvent.ACTION_UP) {
//                    if (mSelectedPos == -1) {
//                        checkSelectedPos();
//                    }
//                    if (mListener != null) {
//                        mListener.onItemClick(mSelectedPos);
//                    }
//                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private void checkSelectedPos() {
        for (int i = 0; i < mDesRects.length; ++i) {
            if (mDesRects[i].contains(mDownX, mDownY)) {
                mSelectedPos = i;
                break;
            }
        }
    }

    private void checkIntersectRect() {
        mIntersectRectPos = -1;
        Rect moveViewRect = new Rect(mLayoutParams.x, mLayoutParams.y, mLayoutParams.x + mWidth / 4, mLayoutParams.y + mWidth / 4);
        int cx = moveViewRect.centerX();
        int cy = moveViewRect.centerY();
        for (int i = 0; i < mDesRects.length; ++i) {
            if (i == mSelectedPos) {
                continue;
            }
            int distance = (int)Math.hypot(mDesRects[i].centerX()-cx, mDesRects[i].centerY()-cy);
            if (Rect.intersects(mDesRects[i], moveViewRect) && distance <= moveViewRect.width()/ 2) {
                mIntersectRectPos = i;
//                Log.v("AAAA:", "mIntersectRectPos: "+mIntersectRectPos);
//                Log.v("AAAA1:", mDesRects[i].toString());
//                Log.v("AAAA2:", moveViewRect.toString());
                break;
            }
        }
    }

    private void adjustAllRects() {
        if (mIntersectRectPos == -1 || mIsAnimRunning) {
            return;
        }
        mIsAnimRunning = true;
        final int startPos, endPos;
        final boolean flag;
        if (mDesRects[mIntersectRectPos].left != mDesRects[mSelectedPos].left) {
            startPos = mDesRects[mIntersectRectPos].left;
            endPos = mDesRects[mSelectedPos].left;
            flag = true;
        } else {
            startPos = mDesRects[mIntersectRectPos].top;
            endPos = mDesRects[mSelectedPos].top;
            flag = false;
        }
        final int selectPos = mSelectedPos;
        mSelectedPos = -1;
        final int width = mDesRects[mIntersectRectPos].width();
        final int diff_top = Math.abs(mDesRects[selectPos].top - mDesRects[mIntersectRectPos].top);
        final int diff_left = Math.abs(mDesRects[selectPos].left - mDesRects[mIntersectRectPos].left);
        final int top = mDesRects[mIntersectRectPos].top;
        final int top1 = mDesRects[selectPos].top;
        final int left = mDesRects[mIntersectRectPos].left;
        final int left1 = mDesRects[selectPos].left;
        ValueAnimator animator = ValueAnimator.ofInt(startPos, endPos);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mIntersectRectPos == -1) return;
                int currPos = (int) animation.getAnimatedValue();
                if (flag) {
                    float currTop = animation.getAnimatedFraction() * diff_top;
//                    currTop = currTop * (startPos < endPos ? -1 : 1);
                    mDesRects[mIntersectRectPos].left = (int)currPos;
                    mDesRects[mIntersectRectPos].top = (int)currTop + top;
                    mDesRects[selectPos].left = endPos + (startPos - currPos);
                    mDesRects[selectPos].top = top1 - (int)currTop;
                } else {
                    float currLeft = animation.getAnimatedFraction() * diff_left;
                    mDesRects[mIntersectRectPos].left = (int)currLeft + left;
                    mDesRects[mIntersectRectPos].top = (int)currPos;
                    mDesRects[selectPos].left = left1 - (int)currLeft;
                    mDesRects[selectPos].top = endPos + (startPos - currPos);
                }
                mDesRects[mIntersectRectPos].right = mDesRects[mIntersectRectPos].left + width;
                mDesRects[mIntersectRectPos].bottom = mDesRects[mIntersectRectPos].top + width;
                mDesRects[selectPos].right = mDesRects[selectPos].left + width;
                mDesRects[selectPos].bottom = mDesRects[selectPos].top + width;
                invalidate();
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mIsAnimRunning = true;
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                mIsAnimRunning = false;
//                Rect tmp = mDesRects[mIntersectRectPos];
//                mDesRects[mIntersectRectPos] = mDesRects[selectPos];
//                mDesRects[selectPos] = tmp;
                mSelectedPos = mIntersectRectPos;
                mIntersectRectPos = -1;
            }
            @Override
            public void onAnimationCancel(Animator animation) {
            }
            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animator.setDuration(300).start();
    }

    public void setBitmap(int pos, Bitmap bitmap) {
        mDesBitmaps[pos] = bitmap;
        mSrcRects[pos].right = bitmap.getWidth();
        mSrcRects[pos].bottom = bitmap.getHeight();
        mSelectedPos = -1;
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mInitBitmap == null) {
            return;
        }
        try {
            canvas.drawBitmap(mDesBitmaps[0], mSrcRects[0], mDesRects[0], null);
            canvas.drawBitmap(mDesBitmaps[1], mSrcRects[1], mDesRects[1], null);
            canvas.drawBitmap(mDesBitmaps[2], mSrcRects[2], mDesRects[2], null);
            canvas.drawBitmap(mDesBitmaps[3], mSrcRects[3], mDesRects[3], null);
            canvas.drawBitmap(mDesBitmaps[4], mSrcRects[4], mDesRects[4], null);
            if (mSelectedPos != -1) {
                canvas.drawRect(mDesRects[mSelectedPos], mPaint);
//                canvas.drawBitmap(mBitmap, mSrcRects[mSelectedPos], mDesRects[mSelectedPos], mPaint);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int pos);
    }
    private OnItemClickListener mListener;
    public void setItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }
}
