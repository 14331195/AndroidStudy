package com.example.administrator.androidstudy.adapter;

import android.graphics.Canvas;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.administrator.androidstudy.R;

/**
 * Created by ljm on 2018/5/8.
 */
public class ItemTouchCallback extends ItemTouchHelper.Callback {
    private ITouchAdapter mAdapter;
    private float mDx = 0;
    private float mSwipeThreshold;
    private int mLength = 0;
    private RecyclerView.ViewHolder mViewHolder;

    public ItemTouchCallback(ITouchAdapter adapter) {
        mAdapter = adapter;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder holder) {
        int moveFlag = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        int swipeFlag = ItemTouchHelper.LEFT;
        return makeMovementFlags(moveFlag, swipeFlag);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder holder, int direction) {
//        mAdapter.onItemDismiss(holder.getAdapterPosition());
//        holder.itemView.setVisibility(View.VISIBLE);
//        holder.itemView.setTranslationX(200);
        View view = holder.itemView.findViewById(R.id.img);
//        holder.itemView.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                holder.itemView.setScrollX(100);
//            }
//        }, 1000);
        Log.v("AAA:", "swipe:"+view.getWidth()+",direction:"+direction);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                       RecyclerView.ViewHolder target) {
        mAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public float getSwipeThreshold(RecyclerView.ViewHolder holder) {
        return mSwipeThreshold;
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        viewHolder.itemView.setScrollX(0);
        super.clearView(recyclerView, viewHolder);
        Log.v("AAA:", "clearView:");
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder holder, int actionState) {
        if (holder == null) return;
        if (holder.itemView.getScrollX() != 0) {
            mSwipeThreshold = 0f;
        } else {
            mSwipeThreshold = 0.25f;
        }
        if (mViewHolder != holder) {
            Log.v("AAA:", "onSelectedChanged1:"+actionState);

        } else {
            Log.v("AAA:", "onSelectedChanged:"+actionState);

        }
    }

    @Override
    public void onChildDraw(Canvas canvas, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                            float dx, float dy, int actionState, boolean isActive) {
        Log.v("AAA:", "onChildDraw dx:"+dx+",dy:"+dy+",actionState:"+actionState+",isActive:"+isActive);
        if (mLength == 0) {
            View imageView = viewHolder.itemView.findViewById(R.id.img);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(recyclerView.getContext(), "111", Toast.LENGTH_SHORT).show();
                }
            });
            mLength = imageView.getMeasuredWidth();
        }

        if (Math.abs(dx) > mLength) {
            mDx = dx;
//            viewHolder.itemView.scrollTo(-(int)dx, 0);
//            super.onChildDraw(canvas, recyclerView, viewHolder, -mLength, dy, actionState, isActive);
        } else {
            viewHolder.itemView.scrollTo(-(int)dx, 0);
//            super.onChildDraw(canvas, recyclerView, viewHolder, dx, dy, actionState, isActive);
        }
//        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
//            viewHolder.itemView.scrollTo(-(int)dx, 0);
////            View imageView = viewHolder.itemView.findViewById(R.id.img);
////            Log.v("AAA:", "w:"+imageView.getWidth()+"|h:"+imageView.getMeasuredWidth());
//        } else {
//            super.onChildDraw(canvas, recyclerView, viewHolder, dx, dy, actionState, isActive);
//        }
    }


    public interface ItemCallback {
        void onItemDismiss(int pos);
        void onItemMove(int pos, int target);
    }
}
