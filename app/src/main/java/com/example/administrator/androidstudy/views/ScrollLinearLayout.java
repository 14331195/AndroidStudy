package com.example.administrator.androidstudy.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.EventLog;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
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
    private RecyclerView mRecyclerView;
    private int mTouchSlop;
    private float mLastY;
    private float mScrollFactor = 0.6f;
    private boolean mScrollUp = false;
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
            mRecyclerView = (RecyclerView)getChildAt(1);
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

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastY = event.getY();
                if (mHeaderLayout.contains((int)event.getRawX(), (int)event.getRawY())) {
                    Log.v("AAA:", "yes");
                } else {
                    Log.v("AAA:", "x:"+(int)event.getX()+"y:"+(int)event.getY()+"|l:"+mHeaderLayout.getLeft()+",r:"+mHeaderLayout.getRight()+",t:"+mHeaderLayout.getTop()+",b:"+mHeaderLayout.getBottom());
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mHeaderLayout.contains((int)event.getRawX(), (int)event.getRawY())) {
                    return false;
                }
                float diff = (event.getY() - mLastY);
                //HeaderLayout还未显示出来
                if (diff > 0 && getScrollY() == 0 && !mRecyclerView.canScrollVertically(-1)) {
//                    boolean s = onTouchEvent(event);
//                    Log.v("AAAA:", "return");
//                    return s;
                    return onTouchEvent(event);
                } else if (Math.abs(getScrollY()) == mHeaderLayout.getHeaderHeight()) {
                    return true;
                }

                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        boolean handled = false;

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mLastY = event.getY();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                Log.v("AAA:", "1");
                mLastY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                handled = true;
                float diff = (event.getY() - mLastY);
                if (diff < 0) {     //向上滑动
                    mScrollUp = true;
                } else {            //向下滑动
                    mScrollUp = false;
                }
//                if (mScrollUp && (getScrollY() == 0 || Math.abs(getScrollY()) < Math.abs(diff))) {
////                    handled = false;
//                    Log.v("AAAA:", "111");
//                    scrollTo(0, 0);
//                    return mRecyclerView.onTouchEvent(event);
//                }
                mLastY = event.getY();

                //下拉时还没滑到headerlayout的顶部 或者 上滑时头部布局还未完全消失
                if ((diff > 0 && mHeaderLayout.canScroll(Math.abs(getScrollY())))
                        || (diff < 0 && getScrollY() < 0 && !mHeaderLayout.isShowing())) {
                    if (diff < 0 ) {
                        diff = Math.abs(getScrollY()) >= Math.abs(diff) ? diff : 0;
                        scrollBy(0, -(int) diff);
                    } else {
                        //判断当前是否会滑到mHeaderLayout的顶部
                        int tmp = (mHeaderLayout.getHeaderHeight() - Math.abs(getScrollY()));
                        diff = diff > tmp ? tmp : diff;
                        //判断当前是否要显示顶部列表
                        diff = Math.abs(getScrollY()) >= mHeaderLayout.getPointHeight() ? diff : diff * mScrollFactor;
                        scrollBy(0, -(int) diff);
                    }
                    mHeaderLayout.onPullDown(Math.abs(getScrollY()));
                } else if(diff > 0 ) {
                    mHeaderLayout.postScaleY(diff * mScrollFactor);
                } else if (diff < 0) {
                    mHeaderLayout.postScaleY(diff * mScrollFactor);
                }

                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (getScrollY() == 0) {
                    return handled;
                }
                if (Math.abs(getScrollY()) >= mHeaderLayout.getPointHeight()) {
                    smoothScroll(getScrollY(), -mHeaderLayout.getHeaderHeight(), 200);
                    Log.v("AAAA:", "ss");
                } else if (mHeaderLayout.canScroll(Math.abs(getScrollY()))) {
                    smoothScroll(getScrollY(), 0, 200);
                    mHeaderLayout.reset();
                    if (mScrollUp) {
                        mHeaderLayout.hidePointView();
                    }
                    Log.v("AAAA:", "aa");
                }
                mHeaderLayout.onCancel();
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
        mSmoothScrollRunnable.set(fromY, toY, duration);
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
                int deltaY = (int)(mInterpolator.getInterpolation(time) * Math.abs(mFromY - mToY));
                mCurrY = mFromY + (mToY >= 0 ? deltaY : deltaY * -1);
//                Log.v("AAAA:", "t:"+(System.currentTimeMillis() - mStartTime) + ",mStartTime: "+mStartTime+",time:"+time + ",mFromY: "+ mFromY+",mToY:"+mToY + ", deltaY:"+deltaY+",mCurrY:"+mCurrY);
                scrollTo(0, mCurrY);
                mHeaderLayout.onPullDown(Math.abs(mCurrY));
            }
            if (mCurrY != mToY) {
                postDelayed(this, 16);
            } else {
                if (mToY == 0) {
                    mHeaderLayout.showPointView();
                } else {
                    mHeaderLayout.hidePointView();
                }
            }
        }

        public void stop() {
            removeCallbacks(this);
        }
        public void set(int fromY, int toY, int duration) {
            mFromY = fromY;
            mToY = toY;
            mDuration = duration;
            mCurrY = -1;
            mStartTime = 0;
        }
    }
}