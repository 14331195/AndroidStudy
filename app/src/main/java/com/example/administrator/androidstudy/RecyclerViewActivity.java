package com.example.administrator.androidstudy;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import com.example.administrator.androidstudy.adapter.ITouchAdapter;
import com.example.administrator.androidstudy.adapter.ItemTouchCallback;
import java.util.ArrayList;
import java.util.List;

/**
 * recyclerview进阶，item的侧滑删除和拖动等，利用ItemTouchHelper类
 */

public class RecyclerViewActivity extends BaseActivity implements View.OnClickListener{


    @Override
    public void onCreate(@Nullable Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);
        setContentView(R.layout.layout_recyclerview_activity);

        RecyclerView recyclerView = $(R.id.recycler_view);
        List<ITouchAdapter.Item> list = new ArrayList<>();
        for (int i = 0; i < 20; ++i) {
            list.add(new ITouchAdapter.Item(i));
        }
        ITouchAdapter adapter = new ITouchAdapter(this, list);
        ItemTouchHelper.Callback callback = new ItemTouchCallback(adapter);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(manager);
        helper.attachToRecyclerView(recyclerView);
    }



    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.save:
                break;
            case R.id.cancel:
                break;
            case R.id.select:
                break;
        }
    }

    @Override
    protected void onActivityResult(int reqCode, int resCode, Intent data) {
        if (reqCode == 0 && resCode == RESULT_OK) {

        }
    }


    @Override
    protected void finalize() throws Throwable{
        super.finalize();
//        Log.v("AAA:", "finalize");
    }

}