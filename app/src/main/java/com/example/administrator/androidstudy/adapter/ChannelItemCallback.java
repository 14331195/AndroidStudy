package com.example.administrator.androidstudy.adapter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;

import com.example.administrator.androidstudy.R;

/**
 * Created by ljm on 2018/5/12.
 */
public class ChannelItemCallback extends ItemTouchHelper.Callback {
    private Context mContext;
    private Paint mPaint;

    public ChannelItemCallback(Context context) {
        this.mContext = context;
        mPaint = new Paint();
        mPaint.setColor(ContextCompat.getColor(mContext, R.color.color1));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(4.0f);
        mPaint.setPathEffect(new DashPathEffect(new float[]{8,4}, 0));

    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
//        mContext = recyclerView.getContext();
        if (!(viewHolder instanceof ChannelAdapter.ChannelHolder) || recyclerView.getAdapter() == null) {
            return makeMovementFlags(0, 0);
        }
        ChannelAdapter adapter  = (ChannelAdapter)recyclerView.getAdapter();
        //只允许已选频道可以发生移动
        if (adapter.isCanMove(viewHolder)) {
            int moveFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
                            | ItemTouchHelper.DOWN | ItemTouchHelper.UP;
            return makeMovementFlags(moveFlags, 0);
        } else {
            return makeMovementFlags(0, 0);
        }
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int actionState) {

    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder fromHolder,
                        RecyclerView.ViewHolder toHolder) {
        ChannelAdapter adapter = (ChannelAdapter)recyclerView.getAdapter();
        if (adapter.isCanMove(toHolder)) {
            adapter.swapItem(fromHolder, toHolder);
            return true;
        }
        return false;
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            ChannelAdapter.ChannelHolder holder = (ChannelAdapter.ChannelHolder)viewHolder;
            holder.tvName.setBackgroundColor(ContextCompat.getColor(mContext, R.color.color2));
            holder.tvDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean isLongPressDragEnabled(){return false;}


    @Override
    public float getMoveThreshold(RecyclerView.ViewHolder viewHolder) {
        return 0.5f;
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        ChannelAdapter.ChannelHolder holder = (ChannelAdapter.ChannelHolder)viewHolder;
        holder.tvName.setBackgroundColor(ContextCompat.getColor(recyclerView.getContext(), R.color.color1));
        holder.tvDelete.setVisibility(View.VISIBLE);
    }

    @Override
    public void onChildDraw(Canvas canvas, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                float dx, float dy, int actionState, boolean isActive) {
        super.onChildDraw(canvas, recyclerView,viewHolder,dx, dy, actionState, isActive);
        ChannelAdapter.ChannelHolder holder = (ChannelAdapter.ChannelHolder) viewHolder;
        if (dx != 0 || dy != 0 || isActive) {   //dx,dy不等于0的判定可以让item动画结束后才让虚线框消失
            canvas.drawRect(holder.itemView.getLeft()+10, holder.itemView.getTop()+10,
                    holder.itemView.getRight()-10,holder.itemView.getBottom()-10, mPaint);
        }
    }
}
