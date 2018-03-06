package com.example.administrator.androidstudy.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.util.EventLog;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * Created by Administrator on 2018/3/5.
 */

public class ScrollLinearLayout extends LinearLayout {
    private HeaderLayout mHeaderLayout;
    private int mTouchSlop;
    private float mLastY;
    private SmoothScrollRunnable mSmoothScrollRunnable;

    public ScrollLinearLayout(Context context) {
        this(context, null, 0);
    }

    public ScrollLinearLayout(Context context, AttributeSet set) {
        this(context, set, 0);
    }

    public ScrollLinearLayout(Context context, AttributeSet set, int defStyle) {
        super(context, set, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        int childCount = getChildCount();
        if (childCount == 2) {
            mHeaderLayout = (HeaderLayout) getChildAt(0);
        }
        init(getContext());
    }

    @TargetApi(16)
    private void init(final Context context) {
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                setPadding(getPaddingLeft(), -mHeaderLayout.getMeasuredHeight(), getPaddingRight(), getPaddingBottom());
                getViewTreeObserver().removeOnGlobalLayoutListener(this);

            }
        });
    }

//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent event) {
//        int action = event.getAction();
//        switch (action) {
//            case MotionEvent.ACTION_DOWN:
//                mLastY = event.getY();
//                break;
//            case MotionEvent.ACTION_MOVE:
//                float diff = Math.abs(event.getY() - mLastY);
//
//                break;
//            case MotionEvent.ACTION_CANCEL:
//            case MotionEvent.ACTION_UP:
//                break;
//        }
//        return false;
//    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        boolean handled = false;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastY = event.getY();
                handled = true;
                break;
            case MotionEvent.ACTION_MOVE:
                float diff = (event.getY() - mLastY);
                mLastY = event.getY();

                if ((diff > 0 && mHeaderLayout.canScroll(Math.abs(getScrollY()))) || (diff < 0 && getScrollY() < 0)) {
                    if (diff < 0 ) {
                        scrollBy(0, -(int) diff);
                    } else {
                        int tmp = (mHeaderLayout.getHeaderHeight() - Math.abs(getScrollY()));
                        diff = diff > tmp ? tmp : diff;
                        scrollBy(0, -(int) diff);
                    }
                    mHeaderLayout.onPullDown(Math.abs(getScrollY()));
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (Math.abs(getScrollY()) >= mHeaderLayout.getPointHeight()) {

                } else if (mHeaderLayout.canScroll(Math.abs(getScrollY()))) {
                    smoothScroll(getScrollY(), 0, 200);
                    mHeaderLayout.reset();
                }
                break;
        }
        return handled;
    }

    private void smoothScroll(int fromY, int toY, int duration) {
        if (mSmoothScrollRunnable != null) {
            mSmoothScrollRunnable.stop();
        } else {
            mSmoothScrollRunnable = new SmoothScrollRunnable(fromY, toY, duration);
        }
        mSmoothScrollRunnable.reset();
        post(mSmoothScrollRunnable);
    }

    private class SmoothScrollRunnable implements Runnable {
        private Interpolator mInterpolator;
        private int mDuration;
        private int mFromY;
        private int mToY;
        private long mStartTime = 0;
        private int mCurrY = -1;

        public SmoothScrollRunnable(int fromY, int toY, int duration) {
            mFromY = fromY;
            mToY = toY;
            mDuration = duration;
            mInterpolator = new DecelerateInterpolator();
        }
        @Override
        public void run() {
            if (mStartTime == 0) {
                mStartTime = System.currentTimeMillis();
            } else {
                float time = (System.currentTimeMillis() - mStartTime) / (float)mDuration;
                time = Math.min(time, 1.0f);
                int deltaY = (int)(mInterpolator.getInterpolation(time) * (mFromY - mToY));
                mCurrY = mFromY - deltaY;
                Log.v("AAAA:", "time: "+ time+","+mInterpolator.getInterpolation(time) + ", deltaY:"+deltaY+",mCurrY:"+mCurrY);
                scrollTo(0, mCurrY);
                mHeaderLayout.onPullDown(Math.abs(getScrollY()));
            }
            if (mCurrY != mToY) {
                postDelayed(this, 16);
            }
        }

        public void stop() {
            removeCallbacks(this);
        }
        public void reset() {
            mCurrY = -1;
            mStartTime = 0;
        }
    }
}
