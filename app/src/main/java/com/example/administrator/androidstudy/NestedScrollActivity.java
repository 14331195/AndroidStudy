package com.example.administrator.androidstudy;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Choreographer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;

import com.example.administrator.androidstudy.adapter.ItemAdapter;
import com.example.administrator.androidstudy.views.ClipCircleImageView;
import com.example.administrator.androidstudy.views.MyProgressView;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/1/10.
 * 嵌套滑动实例，以及仿QQ答题活动下拉背景样式
 */

public class NestedScrollActivity extends BaseActivity{


    @Override
    public void onCreate(@Nullable Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);
        setContentView(R.layout.layout_nested_scroll);


        setupRecyclerView();
        setupViewPager();

        Button button = $(R.id.close);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = $(R.id.nested_fl_2);
                if (view.getVisibility() == View.VISIBLE) {
                    view.setVisibility(View.GONE);
                } else {
                    view.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = $(R.id.recycler_view);
        List<ItemAdapter.Item> list = new ArrayList<>();
        for (int i = 0; i < 200; ++i) {
            list.add(new ItemAdapter.Item(i));
        }
        ItemAdapter adapter = new ItemAdapter(this, list);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
    }

    private void setupViewPager() {
        TabLayout tableLayout = $(R.id.tab_layout);
        ViewPager viewPager = $(R.id.view_pager);

        PagerAdapter pagerAdapter = new FragmentStatePagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return new ListFragment();
            }

            @Override
            public int getCount() {
                return 2;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                switch (position) {
                    case 0: return getString(R.string.str_title1);
                    case 1: return getString(R.string.str_title2);
                }
                return getString(R.string.str_title1);
            }
        };
        viewPager.setAdapter(pagerAdapter);
        tableLayout.setupWithViewPager(viewPager);

    }

    public static class ListFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle bundle){
            View view = inflater.inflate(R.layout.fragment_recyclerview, parent, false);
            RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
            List<ItemAdapter.Item> list = new ArrayList<>();
            for (int i = 0; i < 200; ++i) {
                list.add(new ItemAdapter.Item(i));
            }
            ItemAdapter adapter = new ItemAdapter(getContext(), list);
            LinearLayoutManager manager = new LinearLayoutManager(getContext());
            manager.setOrientation(LinearLayoutManager.VERTICAL);
            recyclerView.setLayoutManager(manager);
            recyclerView.setAdapter(adapter);
            return view;
        }
    }
}