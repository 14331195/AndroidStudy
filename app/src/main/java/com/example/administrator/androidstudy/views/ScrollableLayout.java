package com.example.administrator.androidstudy.views;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.OverScroller;
import android.widget.Scroller;

/**
 * Created by Administrator on 2018/2/2.
 */

public class ScrollableLayout extends LinearLayout {
    private Scroller mScroller;
    private float mLastX;
    private float mLastY;
    private long currTime;
    private static float THRESHOLD = 1.0f;
    private boolean mIsAnimRunning = false;
    private boolean mIsOnLayout = false;

    public ScrollableLayout(Context context) {
        super(context);
    }

    public ScrollableLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mScroller = new Scroller(context);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mLastY = event.getY();
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                mLastY = y;
                Log.v("AAAA:", "pointer");
                break;
            case MotionEvent.ACTION_MOVE:


//                if ((y - mLastY) < THRESHOLD) {
//                    mLastY = y;
//                    return false;
//                }
//                MarginLayoutParams params = (MarginLayoutParams)getLayoutParams();
//                params.topMargin += (y - mLastY);
//                setLayoutParams(params);
//                layout(getLeft(), (int)(getTop() + y - mLastY), getRight(), (int)(getBottom() + y - mLastY));
//                smoothScroll(mLastY - y);
                smoothScroll((int)-(y - mLastY));
                mLastY = y;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                smoothScrollToTop();
                break;
        }
        return super.onTouchEvent(event);
    }

    private void smoothScrollToTop() {
        mScroller.startScroll(0, getScrollY(), 0, 0, 500);
        invalidate();
        Log.v("AAAA:", "aaaa");
//        if (mIsAnimRunning) {
//            return;
//        }
//        ValueAnimator animator = ValueAnimator.ofInt(getTop(), 0);
//        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator animation) {
//                int curr = (int)animation.getAnimatedValue();
//                layout(getLeft(), curr, getRight(), getBottom()-(getTop() - curr));
//            }
//        });
//        animator.setInterpolator(new DecelerateInterpolator());
//        animator.addListener(new Animator.AnimatorListener() {
//            @Override
//            public void onAnimationStart(Animator animation) {
//                mIsAnimRunning = true;
//            }
//
//            @Override
//            public void onAnimationEnd(Animator animation) {
//                mIsAnimRunning = false;
//            }
//
//            @Override
//            public void onAnimationCancel(Animator animation) {
//                mIsAnimRunning = false;
//            }
//
//            @Override
//            public void onAnimationRepeat(Animator animation) {
//
//            }
//        });
//        animator.setDuration(300);
//        animator.start();
    }

    private void smoothScroll(int deltaY) {
        mScroller.startScroll(0, getScrollY(), 0, deltaY, 0);
        invalidate();
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }
}
