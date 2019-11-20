package com.example.administrator.androidstudy.views;

import android.content.Context;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

/**
 * Created by ljm on 2018/5/20.
 * 嵌套滑动示例子view
 */
public class NestedChildView extends TextView implements NestedScrollingChild{
    private NestedScrollingChildHelper mHelper;
    private float mDownX;
    private float mDownY;
    private float mLastX;
    private float mLastY;
    private int[] consumed = new int[2];
    private int[] offsetInWindow = new int[2];

    public NestedChildView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mHelper.isNestedScrollingEnabled();
    }

    /**
     * 此函数会去询问父view是否允许滑动，一般在ACTION_DOWN调用
     * @param axes  滑动的方向
     * @return      返回true表示父view允许子view滑动
     */
    @Override
    public boolean startNestedScroll(int axes) {
        return mHelper.startNestedScroll(axes);
    }

    /**
     * 停止嵌套滑动，一般在ACTION_UP调用
     */
    @Override
    public void stopNestedScroll() {
        mHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mHelper.hasNestedScrollingParent();
    }

    /**
     * 子view滑动后向父view汇报情况
     * @param dxConsumed
     * @param dyConsumed
     * @param dxUnconsumed
     * @param dyUnconsumed
     * @param offsetInWindow    父view的滑动导致子view的位置发生了变化，则子view上一次按下的位置会有改变，需动态计算
     *                          offsetInWindow[0]表示x方向的变化，offsetInWindow[1]表示y方向的变化
     * @return                  true表示父view对滑动做了处理，不然为false
     */
    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed,
                                        int dxUnconsumed,int dyUnconsumed, int[] offsetInWindow) {
        return mHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    /**
     * 子view在滑动之前先向父view汇报情况
     * @param dx                x方向需要滑动的距离
     * @param dy                y方向需要滑动的距离
     * @param consumed          父view的距离消费情况
     * @param offsetInWindow    同上
     * @return                  同上
     */
    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    /**
     *
     * @param velocityX
     * @param velocityY
     * @param consumed      true表示子view消耗fling了
     * @return              true表示父view消耗fling了
     */
    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed){
        return mHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY){
        return mHelper.dispatchNestedPreFling(velocityX, velocityY);
    }


    @Override
    public void onDetachedFromWindow(){
        super.onDetachedFromWindow();
        mHelper.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = mLastX = x;
                mDownY = mLastY = y;
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL | ViewCompat.SCROLL_AXIS_HORIZONTAL);
                break;
            case MotionEvent.ACTION_MOVE:
                int dx = (int)(x - mDownX); //getX方法在view位置发生变化后会跟着变化，
                                            // 因此这里不能用mLastX应该用mDownX，
                                            // 因为mLastX记录的是原来位置的x值，跟变化后的x值是不一致的
                int dy = (int)(y - mDownY);
                //如果父view有消耗，则先减去父view消耗的距离，再交给子view处理剩下的
                if (dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)) {
                    dx -= consumed[0];
                    dy -= consumed[1];
                }
                offsetLeftAndRight(dx);
                offsetTopAndBottom(dy);
                break;
            case MotionEvent.ACTION_UP:
                stopNestedScroll();
                break;
        }
        mLastX = x;
        mLastY = y;
//        Log.v("AAA:", "mDownX:"+mDownX+",mDownY:"+mDownY+"|mLastX:"+mLastX+",mLastY:"+mLastY);
//        Log.v("AAA:", "|mLastX:"+mLastX);
        return true;
    }
}
