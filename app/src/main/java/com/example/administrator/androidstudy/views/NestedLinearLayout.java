package com.example.administrator.androidstudy.views;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.Scroller;

/**
 * Created by ljm on 2018/5/19.
 *
 */
public class NestedLinearLayout extends LinearLayout implements NestedScrollingParent{
    private NestedScrollingParentHelper mHelper;
    private ColorTextView mColorView;
    private ObjectAnimator animator;    //用于松开后回弹的效果实现
    private Scroller mScroller;
    private RecyclerView mChild;
    private int mVelocityX;
    private int mVelocityY;

    private RecyclerView.OnScrollListener listener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState == RecyclerView.SCROLL_STATE_IDLE &&
                    mChild != null && !mChild.canScrollVertically(-1)) {
                mScroller.fling(getScrollX(), getScrollY(), (int)mVelocityX, (int)mVelocityY,
                        0, 0, -100, 0);
                postInvalidate();
            }
        }
    };

    public NestedLinearLayout(Context context) {
        super(context);
    }

    public NestedLinearLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    private void init() {
        mScroller = new Scroller(getContext());
        mHelper = new NestedScrollingParentHelper(this);
        mColorView = (ColorTextView) getChildAt(0);
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                setPadding(0,-mColorView.getHeight(), 0, 0);
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        init();
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
        if (animator != null) {
            animator.cancel();
        }
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

        Log.v("AAA:", "onStopNestedScroll");
        if (animator == null) {
            animator = ObjectAnimator.ofInt(this, "ScrollY", getScrollY(), 0);
            animator.setDuration(300);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mColorView.drawColorBg(getScrollY());
                }
            });
        } else {
            animator.setIntValues(getScrollY(), 0);
        }

        animator.start();
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
        if (velocityY > 0) return false;
        mChild = (RecyclerView)target;
        mVelocityX = (int)velocityX;
        mVelocityY = (int)velocityY;
        mChild.addOnScrollListener(listener);
        return false;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int currScrollY = mScroller.getCurrY();
            scrollTo(0, currScrollY);
            mColorView.drawColorBg(currScrollY);
            postInvalidate();
        } else {

        }
        if (mChild != null) {
            mChild.removeOnScrollListener(listener);
        }
    }

    /**
     * 子view在fling前的情况
     * @param target
     * @param velocityX
     * @param velocityY
     * @return
     */
    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        return false;
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
//        Log.v("nestedScrollLL AAA:", "scrollY:"+target.getScrollY()+"|dx:"+dx
//                +"|dy:"+dy+"|consumed:"+consumed[0]+","+consumed[1]);
        final RecyclerView child = (RecyclerView) target;
        if (dy <= 0) {   //向下滑动
            if (!child.canScrollVertically(-1)) {       //RecyclerView已经滑到顶部
                scrollBy(0, -Math.abs((int)(dy*0.6)));
                mColorView.drawColorBg(getScrollY());
                consumed[1] += dy;
            } else {

            }
        } else {    //向上滑动
            int scrollY = Math.abs(getScrollY());
            if (scrollY > 0) {
                if (dy <= scrollY) {
                    scrollBy(0, dy);
                } else {
                    dy = scrollY;
                    scrollTo(0, 0);
                }
                mColorView.drawColorBg(getScrollY());
                consumed[1] += dy;
            } else {
            }
        }
    }

}
