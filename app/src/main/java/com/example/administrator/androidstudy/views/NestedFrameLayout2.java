package com.example.administrator.androidstudy.views;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.design.widget.TabLayout;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.OverScroller;
import android.widget.TextView;

import com.example.administrator.androidstudy.R;

/**
 * Created by ljm on 2018/5/19.
 * 嵌套滑动加ViewPager示例
 */
public class NestedFrameLayout2 extends FrameLayout implements NestedScrollingParent{
    private NestedScrollingParentHelper mHelper;

    private TextView mTvTop;
    private View mView;
    private View mFlHeader;
    private View mContent;
    private TabLayout mTabLayout;
    private int mMaxScroll;     //最大可向上滑动的距离
    private int mHeaderHeight;  //顶部view的高度
    private RecyclerView mRecyclerView;
    private ViewPager mViewPager;
    private OverScroller mScroller;
    private VelocityTracker mVelocityTracker;
    private int mMaxFlingVelocity;
    private int mMinFlingVelocity;
    private int mDy;        //记录fling时的move事件，防止fling的方向跟move的方向不一致
    private boolean misFinishedFling;

    public NestedFrameLayout2(Context context) {
        super(context);
    }

    public NestedFrameLayout2(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    private void init() {
        mHelper = new NestedScrollingParentHelper(this);
        mScroller = new OverScroller(getContext());
        mVelocityTracker = VelocityTracker.obtain();
        mMaxFlingVelocity = ViewConfiguration.get(getContext()).getScaledMaximumFlingVelocity();
        mMinFlingVelocity = ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity();
        mTvTop = (TextView) findViewById(R.id.tv_top);
        mContent = findViewById(R.id.ll_content);
        mView = findViewById(R.id.view);
        mViewPager = (ViewPager)findViewById(R.id.view_pager);
        mFlHeader = findViewById(R.id.fl_header);
        mTabLayout = (TabLayout)findViewById(R.id.tab_layout);
        mTvTop.setAlpha(0);
        mTvTop.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mRecyclerView = (RecyclerView)findViewById(R.id.recycler_view);

                mMaxScroll = mTabLayout.getTop() - mTvTop.getHeight();
                Log.v("AAA:", "maxscroll:"+mMaxScroll);
                mTvTop.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        init();
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        super.onMeasure(widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(getMeasuredHeight()+ mFlHeader.getMeasuredHeight(), heightSpecMode));
    }

    /**
     *
     * @param child             子view，因为实现嵌套滑动的子view不需要是父view的直接子view
     * @param target            实现嵌套滑动的子view
     * @param nestedScrollAxes  滑动方向
     * @return
     */
    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return true;
    }

    /**
     * onStartNestedScroll返回true，该函数就会被调用
     * @param child
     * @param target
     * @param nestedScrollAxes
     */
    @Override
    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
        mHelper.onNestedScrollAccepted(child, target, nestedScrollAxes);
    }

    @Override
    public int getNestedScrollAxes() {
        return mHelper.getNestedScrollAxes();
    }

    @Override
    public void onStopNestedScroll(View target) {
        mHelper.onStopNestedScroll(target);
    }

    /**
     * 嵌套滑动的子view在滑动后消耗的情况
     * @param target
     * @param dxConsumed    子view在x轴已消耗的距离
     * @param dyConsumed
     * @param dxUnsonsumed
     * @param dyUnsonsumed
     */
    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnsonsumed, int dyUnsonsumed) {
    }

    /**
     *  子view在fling后的情况
     * @param target
     * @param velocityX
     * @param velocityY
     * @param consumed  子view是否消耗fling
     * @return          返回值表示父view是否消耗了fling
     */
    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        return false;
    }

    /**
     * 子view在fling前的情况
     * @param target
     * @param velocityX
     * @param velocityY     往上滑动为正，往下为负数
     * @return
     */
    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        mRecyclerView = (RecyclerView)target;
        //得到的fling方向跟滑动方向不一致的话，按滑动方向的为准
        if (mDy > 0 && velocityY < 0 || mDy < 0 && velocityY > 0) {
            velocityY = -velocityY;
        }
        misFinishedFling = false;
        if (velocityY < 0) {    //往下
//            Log.v("AAA:", "down"+", scrollY:"+mContent.getScrollY());
            //mRecyclerView尚未滑到顶部
//            if (ViewCompat.canScrollVertically(mRecyclerView, -1)) return false;
            if (!mScroller.isFinished())mScroller.forceFinished(false);
            mScroller.fling(0, mContent.getScrollY(), 0, (int) velocityY, 0, 0, -10000, mMaxScroll * 2);
            invalidate();
            if (mContent.getScrollY() != mMaxScroll) {
                mDy = 0;
                return true;
            } else {
                return false;
            }
        } else {
//            Log.v("AAA:", "up"+", scrollY:"+mContent.getScrollY());
//            if (!(mRecyclerView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE)) return false;
            if (mContent.getScrollY() == mMaxScroll) return false;
            if (!mScroller.isFinished())mScroller.forceFinished(false);
            Log.v("AAA:", "velocityY:"+velocityY);
            velocityY = Math.min(Math.max(velocityY, 6000), 15000);
            mScroller.fling(0, mContent.getScrollY(), 0, (int) velocityY/3, 0, 0, 0, mMaxScroll * 3);
        }
        return true;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
//            Log.v("AAA:", "curr:"+mScroller.getCurrY()+",scrollY:"+mContent.getScrollY()+",mDy:"+mDy);
            //这里用来实现RecyclerView下滑到顶部后，整体布局会跟着fling一段距离
            if (mDy < 0){
                if (ViewCompat.canScrollVertically(mRecyclerView, -1)) {//mRecyclerView尚未滑到顶部
                    invalidate();
                    return;
                } else {
                    float velocityY = Math.min(mScroller.getCurrVelocity(), 300);
//                    mScroller.setFriction(ViewConfiguration.getScrollFriction() * Math.max(velocityY/100, 1));
                    mScroller.fling(0, mContent.getScrollY(), 0, (int) -velocityY, 0, 0, -10000, mMaxScroll);
                    mDy = 0;
                }
            }

            int scrollY = mContent.getScrollY();
            if (mScroller.getCurrY() > scrollY) {  //往上fling
                if (mContent.getScrollY() < mMaxScroll) {
                    mContent.scrollTo(mScroller.getCurrX(),
                            Math.min(mScroller.getCurrY(), mMaxScroll));
                    onScrollUp(mContent.getScrollY() - scrollY);
                    invalidate();
                } else {
                    mRecyclerView.fling(0, (int)(mScroller.getCurrVelocity()/2));
                    Log.v("AAA:", "fling");
                }
            } else {    //往下fling
                if (mContent.getScrollY() > 0 && (mRecyclerView.getScrollState()==RecyclerView.SCROLL_STATE_IDLE)) {
                    mContent.scrollTo(mScroller.getCurrX(),
                            Math.max(mScroller.getCurrY(),0));
                    onScrollDown(mContent.getScrollY() - scrollY);
                    invalidate();
                }
            }
        }
    }

    /**
     * 子view滑动前先报告父view
     * @param target
     * @param dx        子view在水平方向想要滑动的距离
     * @param dy        子view在竖直方向想要滑动的距离
     * @param consumed  consumed[0]表示父view在水平方向已消耗的距离，在实现该函数时需要给其复制，告诉子view
     */
    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        mDy = dy;
        if (dy <= 0) {   //向下滑动
            if(mContent.getScrollY() > 0 && !ViewCompat.canScrollVertically(target, -1)) {
                if (Math.abs(dy) > mContent.getScrollY()) {
                    dy = -mContent.getScrollY();
                }
                mContent.scrollBy(0, dy);
                consumed[1] += dy;
            }

            onScrollDown(dy);
        } else {        //向上滑动
            if (mContent.getScrollY() < mMaxScroll) {
                if (dy > mMaxScroll - mContent.getScrollY()) {  //当前滑动的距离超过剩余可滑动距离
                    dy = mMaxScroll - mContent.getScrollY();
                }
                mContent.scrollBy(0, dy);
                consumed[1] += dy;

                onScrollUp(dy);
//                if (mContent.getScrollY() >= mMaxScroll / 4) {
//                    mView.offsetTopAndBottom(-(int)(dy * 0.4));
//
//                    float fraction = (mContent.getScrollY() - mMaxScroll / 4) / (mMaxScroll*3/4.0f);
//                    mView.setScaleX(1 - Math.min(0.5f, Math.abs(fraction) * 0.5f));
//                    mView.setScaleY(mView.getScaleX());
//                    mView.setAlpha(1 - fraction);
//                }
            }

        }
    }

    private void onScrollDown(int dy) {
        if (mView.getBottom() < mTabLayout.getTop()) {
            int offset = mTabLayout.getTop() - mView.getBottom();
            offset = Math.min(offset, -(int)(dy * 0.5));
            mView.offsetTopAndBottom(offset);
        } else {
            mView.setScaleX(1);
            mView.setScaleY(mView.getScaleX());
            mView.setAlpha(1);
        }
        if (mContent.getScrollY() > mMaxScroll / 4) {
            float fraction = (mContent.getScrollY() - mMaxScroll / 4) / (mMaxScroll*3/4.0f);
            mView.setScaleX(0.5f + Math.min(0.5f, Math.abs(1 - fraction) * 0.5f));
            mView.setScaleY(mView.getScaleX());
            mView.setAlpha(1 - fraction);
        }

        //改变顶部View的背景透明度
        float percentage = mContent.getScrollY() / (float)mMaxScroll;
        mTvTop.setAlpha(Math.max(0f, percentage));
    }

    private void onScrollUp(int dy) {
        if (mContent.getScrollY() >= mMaxScroll / 4) {
            mView.offsetTopAndBottom(-(int)(dy * 0.5));

            float fraction = (mContent.getScrollY() - mMaxScroll / 4) / (mMaxScroll*3/4.0f);
            mView.setScaleX(1 - Math.min(0.5f, Math.abs(fraction) * 0.5f));
            mView.setScaleY(mView.getScaleX());
            mView.setAlpha(1 - fraction);
        }

        //改变顶部View的背景透明度
        float percentage = mContent.getScrollY() / (float)mMaxScroll;
        mTvTop.setAlpha(Math.max(0f, percentage));
    }

    private int mDownX, mDownY;
    private int mLastX, mLastY;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownY = mLastY = y;
                mVelocityTracker.addMovement(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                mVelocityTracker.addMovement(event);
                int diff = y - mLastY;
                onNestedPreScroll(mRecyclerView, 0, -(int)(diff), new int[2]);
                break;
            case MotionEvent.ACTION_UP:
                mVelocityTracker.addMovement(event);
                mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                float velocityY = VelocityTrackerCompat.getYVelocity(mVelocityTracker,
                        event.getPointerId(MotionEventCompat.getActionIndex(event)));
                mScroller.fling(0, mContent.getScrollY(), 0, (int) -velocityY, 0, 0, 0, mMaxScroll * 3);
                mVelocityTracker.clear();
                break;
        }
        mLastY = y;
        return false;
    }

    private boolean isTabLayoutOnClick;
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
//        Log.v("AAA:","event:"+event.toString());
        float x = event.getX();
        float y = event.getY();
        int[] loc = new int[2];
        mTabLayout.getLocationOnScreen(loc);
        RectF rectF = new RectF(loc[0], loc[1], loc[0]+mTabLayout.getWidth(), loc[1]+mTabLayout.getHeight());

        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            mVelocityTracker.addMovement(event);
            //需要考虑第一次按下按在TabLayout上，但是move过程中，move到了TabLayout的外面，这种情况也要拦截move事件
            if (!rectF.contains(x, y) && !isTabLayoutOnClick) {
                return false;
            } else {
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mVelocityTracker.addMovement(event);
            mLastY = (int)event.getY();
            if (rectF.contains(x, y)) {     //点中TabLayout
                isTabLayoutOnClick = true;
            } else {
                isTabLayoutOnClick = false;
            }
        }
        return super.onInterceptTouchEvent(event);
    }

}
