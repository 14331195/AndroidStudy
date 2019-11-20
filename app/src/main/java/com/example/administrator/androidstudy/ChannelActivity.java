package com.example.administrator.androidstudy;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;

import com.example.administrator.androidstudy.adapter.ChannelAdapter;
import com.example.administrator.androidstudy.adapter.ChannelItemCallback;
import com.example.administrator.androidstudy.adapter.ITouchAdapter;
import com.example.administrator.androidstudy.adapter.ItemTouchCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * 仿腾讯新闻客户端频道选择界面，利用ItemTouchHelper类
 */

public class ChannelActivity extends BaseActivity{


    @Override
    public void onCreate(@Nullable Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);
        setContentView(R.layout.layout_channel);

        RecyclerView recyclerView = $(R.id.recycler_view);
        List<ChannelAdapter.Item> list1 = new ArrayList<>();
        for (int i = 0; i < 1 + 20 + 30 + 31; ++i) {
            if (i == 0) {
                list1.add(new ChannelAdapter.Item(getString(R.string.str_news), ChannelAdapter.Item.ITEM_TYPE_UNSELECTED));
            } else if (i < 1 + 20) {
                list1.add(new ChannelAdapter.Item("P"+i, ChannelAdapter.Item.ITEM_TYPE_SELECTED));
                list1.get(list1.size() - 1).extra_type = ChannelAdapter.Item.ITEM_TYPE_RECOMMENDED;
            } else if (i < 1 + 20 + 30) {
                list1.add(new ChannelAdapter.Item("R"+i, ChannelAdapter.Item.ITEM_TYPE_RECOMMENDED));
                list1.get(list1.size() - 1).extra_type = ChannelAdapter.Item.ITEM_TYPE_RECOMMENDED;
            } else {
                list1.add(new ChannelAdapter.Item("L"+i, ChannelAdapter.Item.ITEM_TYPE_LOCALNEWS));
                list1.get(list1.size() - 1).extra_type = ChannelAdapter.Item.ITEM_TYPE_LOCALNEWS;
            }
        }
        ChannelAdapter adapter = new ChannelAdapter(this, list1, 1, 20, 30, 31);
        GridLayoutManager manager = new GridLayoutManager(this, 4);
        manager.setOrientation(GridLayoutManager.VERTICAL);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position == 0 || adapter.getItemViewType(position) == ChannelAdapter.TYPE_TITLE_RECOMMENDED) {
                    return 4;
                } else {
                    return 1;
                }
            }
        });
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);

        ChannelItemCallback channelItemCallback = new ChannelItemCallback(this);
        ItemTouchHelper helper = new ItemTouchHelper(channelItemCallback);
        helper.attachToRecyclerView(recyclerView);
        adapter.setmItemTouchHelper(helper);
    }



}