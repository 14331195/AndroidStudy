package com.example.administrator.androidstudy.adapter;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.example.administrator.androidstudy.R;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by ljm on 2018/5/11.
 */
public class ChannelAdapter extends RecyclerView.Adapter {
    public static class Item {
        public Item(String name, int type) {this.name = name; this.type = type;}
        public String name;
        public int type;        //用于列表的展示
        public int extra_type;  //用于判断当前item是从推荐频道来的还是地方新闻
        public static final int ITEM_TYPE_UNSELECTED = 0;   //不可选择的item
        public static final int ITEM_TYPE_SELECTED = 1;     //已选择的item
        public static final int ITEM_TYPE_RECOMMENDED = 2;  //推荐频道
        public static final int ITEM_TYPE_LOCALNEWS = 3;    //地方新闻
    }
    public static final int TYPE_TITLE_SELECTED = 0;       //已选频道标题
    public static final int TYPE_TITLE_RECOMMENDED = 1;    //推荐频道和地方新闻
    public static final int TYPE_ITEM = 2;                 //普通item
    public static final int TAB_RECOMMENDED = 0;           //推荐频道tab
    public static final int TAB_LOCALNEWS = 1;             //地方新闻tab
    private List<Item> mList;
    private Context mContext;
    private LayoutInflater mInflater;
    private int mCurrTab = TAB_RECOMMENDED;
    public int mUnselectedSize;    //不可选择的item数量
    public int mSelectedSize;      //已选择的item数量
    public int mRecommendedSize;   //推荐频道的item数量
    public int mLocalNewsSize;     //地方新闻的item数量
    private float mRecommendTabX;  //推荐频道tab的x坐标
    private float mLocalNewsTabX;  //地方新闻tab的x坐标
    private float mTabY;           //下方两个tab的Y坐标，用于删除item的动画执行

    private ItemTouchHelper mItemTouchHelper;   //用于实现item的拖拽，不需要长按才能拖拽，点击即可拖拽

    public ChannelAdapter(Context context, List<Item> list, int mUnselectedSize, int mSelectedSize,
                          int mRecommendedSize, int mLocalNewsSize) {
        this.mContext = context;
        mInflater = LayoutInflater.from(context);
        this.mList = list;
        this.mUnselectedSize = mUnselectedSize;
        this.mSelectedSize = mSelectedSize;
        this.mRecommendedSize = mRecommendedSize;
        this.mLocalNewsSize = mLocalNewsSize;
    }

    public void setmItemTouchHelper(ItemTouchHelper helper) {
        this.mItemTouchHelper = helper;
    }

    @Override
    public int getItemCount() {
        if (mList == null) {    //这里需要额外加上2，因为有两个标题
            return 2;
        } else if (mCurrTab == TAB_RECOMMENDED) {   //点击不同的tab会返回不同的数据集大小
            return mList.size() - mLocalNewsSize + 2;
        } else if (mCurrTab == TAB_LOCALNEWS) {
            return mList.size() - mRecommendedSize + 2;
        } else {
            return mList.size() + 2;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_TITLE_SELECTED;
        } else if (position == mUnselectedSize + mSelectedSize + 1) {
            return TYPE_TITLE_RECOMMENDED;
        } else {
            return TYPE_ITEM;
        }
    }

    /**
     * 判断当前选中的viewholder能否移动，即有些固定item不可移动
     * @param holder
     * @return
     */
    public boolean isCanMove(RecyclerView.ViewHolder holder) {
        int pos = holder.getLayoutPosition();
        if (pos >= mUnselectedSize + 1 && pos <= mUnselectedSize + mSelectedSize) {
            return true;
        }
        return false;
    }

    public void swapItem(RecyclerView.ViewHolder fromHolder, RecyclerView.ViewHolder toHolder) {
        if (fromHolder.getLayoutPosition() > toHolder.getLayoutPosition()) {
            for (int i = fromHolder.getLayoutPosition()-1; i > toHolder.getLayoutPosition()-1; --i) {
                Collections.swap(mList, i, i - 1);
            }
        } else {
            for (int i = fromHolder.getLayoutPosition()-1; i < toHolder.getLayoutPosition()-1; ++i) {
                Collections.swap(mList, i, i + 1);
            }
        }
        notifyItemMoved(fromHolder.getLayoutPosition(), toHolder.getLayoutPosition());
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        switch (viewType) {
            case TYPE_TITLE_SELECTED:
                View view = mInflater.inflate(R.layout.item_channel_selected, parent, false);
                viewHolder = new RecyclerView.ViewHolder(view){};
                break;
            case TYPE_TITLE_RECOMMENDED:
                view = mInflater.inflate(R.layout.item_channel_recommend, parent, false);
                viewHolder = new TitleHolder(view);
                break;
            case TYPE_ITEM:
                view = mInflater.inflate(R.layout.item_channel, parent, false);
                viewHolder = new ChannelHolder(view);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ChannelHolder) {
            ((ChannelHolder) holder).bindData(position);
        }
    }

    public class ChannelHolder extends RecyclerView.ViewHolder {
        public TextView tvName;
        public TextView tvDelete;
        public ChannelHolder(View view) {
            super(view);
            tvName = (TextView)view.findViewById(R.id.tv_name);
            tvDelete = (TextView)view.findViewById(R.id.tv_delete);
        }

        public void bindData(int position) {
            Item item;
            if (position < mUnselectedSize + mSelectedSize + 1) {
                item = mList.get(position - 1);
            } else {
                if (mCurrTab == TAB_RECOMMENDED) {
                    item = mList.get(position - 2);
                } else {
                    item = mList.get(position - 2 + mRecommendedSize);
                }
            }
            switch (item.type) {
                case Item.ITEM_TYPE_UNSELECTED:
                    tvName.setTextColor(ContextCompat.getColor(mContext, R.color.gray));
                    tvDelete.setVisibility(View.GONE);
                    break;
                case Item.ITEM_TYPE_SELECTED:
                    tvName.setTextColor(ContextCompat.getColor(mContext, R.color.black));
                    tvDelete.setVisibility(View.VISIBLE);
                    break;
                case Item.ITEM_TYPE_RECOMMENDED:
                case Item.ITEM_TYPE_LOCALNEWS:
                    tvName.setTextColor(ContextCompat.getColor(mContext, R.color.black));
                    tvDelete.setVisibility(View.GONE);
                    break;
            }
            tvName.setText(item.name);
            tvDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tvDelete.setVisibility(View.GONE);
                    if (item.extra_type == Item.ITEM_TYPE_RECOMMENDED) {    //当前item是推荐频道的item
                        item.type = Item.ITEM_TYPE_RECOMMENDED;
                        if (mCurrTab == TAB_RECOMMENDED) {
                            //这里需要用getLayoutPosition，因为position会一直保持移动前的值
                            moveItem(getLayoutPosition(), mUnselectedSize + mSelectedSize, false);
                        } else {
                            moveItem(getLayoutPosition(), mUnselectedSize + mSelectedSize, true);
                        }
                        --mSelectedSize;
                        ++mRecommendedSize;
                    } else {    //当前item是地方新闻的item
                        item.type = Item.ITEM_TYPE_LOCALNEWS;
                        if (mCurrTab == TAB_LOCALNEWS) {
                            //这里需要用getLayoutPosition，因为position会一直保持移动前的值
                            moveItem(getLayoutPosition(), mUnselectedSize + mSelectedSize + mRecommendedSize, false);
                        } else {
                            moveItem(getLayoutPosition(), mUnselectedSize + mSelectedSize + mRecommendedSize, true);
                        }
                        --mSelectedSize;
                        ++mLocalNewsSize;
                    }
                }
            });
            this.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (item.type == Item.ITEM_TYPE_UNSELECTED) return;
                    if (item.type == Item.ITEM_TYPE_SELECTED ) {
                        tvDelete.performClick();
                        return;
                    }
                    if (item.type == Item.ITEM_TYPE_RECOMMENDED) {
                        moveItem(getLayoutPosition(), mUnselectedSize + mSelectedSize, false);
                        --mRecommendedSize;
                    } else {
                        moveItem(getLayoutPosition() + mRecommendedSize, mUnselectedSize + mSelectedSize, false);
                        --mLocalNewsSize;
                    }
                    ++mSelectedSize;
                    item.type = Item.ITEM_TYPE_SELECTED;
                    tvDelete.setVisibility(View.VISIBLE);
                }
            });
            tvName.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        mItemTouchHelper.startDrag(ChannelHolder.this);
                        return true;
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        itemView.performClick();
                    }
                    return false;
                }
            });
        }

        /**
         *
         * @param fromPosition
         * @param toPosition
         * @param isPlayDeleteAnim  表示是否执行自定义的删除动画，因为删除的item不在当前显示的频道时需要 执行动画
         */
        private void moveItem(int fromPosition, int toPosition, boolean isPlayDeleteAnim) {
            if (fromPosition <= toPosition) {
                for (int i = fromPosition - 1; i < toPosition - 1; ++i) {   //fromPosition是getLayoutPosition获取的，因此在列表中的实际位置需要减去1(因为上面有个标题)
                    Collections.swap(mList, i, i + 1);
                }
            } else {
                for (int i = fromPosition - 2; i > toPosition; --i) {
                    Collections.swap(mList, i, i - 1);
                }
            }
            if (isPlayDeleteAnim) {
//                performDeleteAnim();
                notifyItemMoved(getLayoutPosition(), mUnselectedSize + mSelectedSize);
                notifyItemRemoved(mUnselectedSize + mSelectedSize);
            } else {
                if (mCurrTab == TAB_LOCALNEWS) {
                    notifyItemMoved(getLayoutPosition(), mUnselectedSize + mSelectedSize + 1);
                } else {
                    notifyItemMoved(getLayoutPosition(), toPosition + 1);
                }
            }
        }

        private void performDeleteAnim() {
            float endX;
            if (mCurrTab == TAB_RECOMMENDED) {
                endX = mLocalNewsTabX;
            } else {
                endX = mRecommendTabX;
            }
            ObjectAnimator transX = ObjectAnimator.ofFloat(this.itemView, "translationX", 0, endX - itemView.getLeft());
            ObjectAnimator transY = ObjectAnimator.ofFloat(this.itemView, "translationY", 0, mTabY - itemView.getTop());
            AnimatorSet set = new AnimatorSet();
            set.setDuration(200);
            set.play(transX).with(transY);
            set.start();
        }
    }

    private class TitleHolder extends RecyclerView.ViewHolder {
        public TitleHolder(View view) {
            super(view);
            View llRecommended = view.findViewById(R.id.ll_recommend);
            View llLocalNews = view.findViewById(R.id.ll_local_news);
            TextView tvRecommended = (TextView) view.findViewById(R.id.tv_recommend);
            TextView tvLocalNews = (TextView) view.findViewById(R.id.tv_local_news);
            TextView tvUnderline1 = (TextView)view.findViewById(R.id.tv_underline1);
            TextView tvUnderline2 = (TextView)view.findViewById(R.id.tv_underline2);
            mRecommendTabX = llRecommended.getLeft();
            mLocalNewsTabX = llLocalNews.getLeft();
            itemView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mTabY = itemView.getTop() - 50;
                    return true;
                }
            });

            llRecommended.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCurrTab == TAB_RECOMMENDED) return;
                    tvRecommended.setTextColor(ContextCompat.getColor(mContext, R.color.black));
                    tvLocalNews.setTextColor(ContextCompat.getColor(mContext, R.color.gray));
                    tvUnderline1.setVisibility(View.VISIBLE);
                    tvUnderline2.setVisibility(View.GONE);
                    mCurrTab = TAB_RECOMMENDED;
                    //先remove，防止报内外数据集不一致的异常
                    notifyItemRangeRemoved(2 + mUnselectedSize + mSelectedSize, mLocalNewsSize);
                    notifyItemRangeChanged(2 + mUnselectedSize + mSelectedSize, mLocalNewsSize);
                }
            });
            llLocalNews.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCurrTab == TAB_LOCALNEWS) return;
                    tvRecommended.setTextColor(ContextCompat.getColor(mContext, R.color.gray));
                    tvLocalNews.setTextColor(ContextCompat.getColor(mContext, R.color.black));
                    tvUnderline1.setVisibility(View.GONE);
                    tvUnderline2.setVisibility(View.VISIBLE);
                    mCurrTab = TAB_LOCALNEWS;
                    //先remove，防止报内外数据集不一致的异常
                    notifyItemRangeRemoved(2 + mUnselectedSize + mSelectedSize, mRecommendedSize);
                    notifyItemRangeChanged(2 + mUnselectedSize + mSelectedSize, mRecommendedSize);
                }
            });
        }
    }

}
