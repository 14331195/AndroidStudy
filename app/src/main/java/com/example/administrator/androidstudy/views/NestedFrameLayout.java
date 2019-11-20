package com.example.administrator.androidstudy.views;

import android.content.Context;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * Created by ljm on 2018/5/19.
 * 嵌套滑动
 */
public class NestedFrameLayout extends FrameLayout implements NestedScrollingParent{
    private NestedScrollingParentHelper mHelper;

    public NestedFrameLayout(Context context) {
        super(context);
    }

    public NestedFrameLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    private void init() {
        mHelper = new NestedScrollingParentHelper(this);
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
//        Log.v("AAA:", "dx:"+dx);
        if (dx <= 0) {   //向左滑动
            if (target.getLeft() + dx < 0) {    //要滑动的距离大于父view与子view左边界的间距
                dx += target.getLeft();
                offsetLeftAndRight(dx);
                consumed[0] += dx;
            }
        } else {        //向右滑动
            if (dx + target.getRight() > getWidth()) {
                dx = dx + target.getRight() - getWidth();
                offsetLeftAndRight(dx);
                consumed[0] += dx;
            }
        }

        if (dy <= 0) {   //向上滑动
            if (dy + target.getTop() < 0) {
                dy += target.getTop();
                offsetTopAndBottom(dy);
                consumed[1] += dy;
            }
        } else {        //向下滑动
            if (dy + target.getBottom() > getHeight()) {
                dy = dy + target.getBottom() - getHeight();
                offsetTopAndBottom(dy);
                consumed[1] += dy;
            }
        }
    }

}
