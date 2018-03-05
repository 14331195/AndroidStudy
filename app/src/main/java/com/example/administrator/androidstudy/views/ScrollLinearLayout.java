package com.example.administrator.androidstudy.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.util.EventLog;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

/**
 * Created by Administrator on 2018/3/5.
 */

public class ScrollLinearLayout extends LinearLayout {
    private HeaderLayout mHeaderLayout;
    private int mTouchSlop;
    private float mLastY;

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
    private void init(Context context) {
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                setPadding(getPaddingLeft(), mHeaderLayout.getMeasuredHeight(), getPaddingRight(), getPaddingBottom());
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
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float diff = Math.abs(event.getY() - mLastY);

                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                break;
        }
        return false;
    }
}
