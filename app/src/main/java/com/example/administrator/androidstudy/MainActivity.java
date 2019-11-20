package com.example.administrator.androidstudy;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.administrator.androidstudy.adapter.ChannelAdapter;
import com.example.administrator.androidstudy.adapter.ItemAdapter;
import com.example.administrator.androidstudy.views.ClipCircleImageView;
import com.example.administrator.androidstudy.views.ClipImageView;
import com.example.administrator.androidstudy.views.MyProgressView;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/1/10.
 */

public class MainActivity extends BaseActivity implements View.OnClickListener, Choreographer.FrameCallback{
    private Button cancel;
    private Button save;
    private Button select;
    private ClipCircleImageView mImageView;
    private ImageView mClipImageView;
    private int progress = 10;
    private int frameNum = 0;
    private MyProgressView myProgressView;

    @Override
    public void onCreate(@Nullable Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);
        setContentView(R.layout.layout);

//        cancel = $(R.id.cancel);
//        select = $(R.id.select);
//        save = $(R.id.save);
//        mImageView = $(R.id.clip_image);
//        mClipImageView = $(R.id.result);
//        mImageView.post(new Runnable() {
//            @Override
//            public void run() {
//                mImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ss0));
//            }
//        });
//        cancel.setOnClickListener(this);
//        select.setOnClickListener(this);
//        save.setOnClickListener(this);
//        select.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View v) {
//                Intent intent = new Intent(MainActivity.this, AIDLActivity.class);
//                startActivity(intent);
//                return true;
//            }
//        });
//        setupRecyclerView();
//        Choreographer.getInstance().postFrameCallback(this);
//        select.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                frameNum = 0;
//                select.postDelayed(this, 1000);
//            }
//        }, 1000);
    }


    private void setupRecyclerView() {
        RecyclerView recyclerView = $(R.id.recycler_view);
        List<ItemAdapter.Item> list = new ArrayList<>();
        for (int i = 0; i < 20; ++i) {
            list.add(new ItemAdapter.Item(i));
        }
        list.get(0).text = "photo";
        list.get(1).text = "left delete";
        list.get(2).text = "itemtouchhelper";
        list.get(3).text = "nested move";
        ItemAdapter adapter = new ItemAdapter(this, list);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL));

        RecyclerView listView = $(R.id.list_view);
        manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        listView.setLayoutManager(manager);
        listView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));


    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.save:
                mClipImageView.setVisibility(View.VISIBLE);
                mClipImageView.setImageBitmap(mImageView.clipImage());
                break;
            case R.id.cancel:
                mClipImageView.setVisibility(View.GONE);
                break;
            case R.id.select:
                myProgressView.setProgress(++progress);

//                Intent intent = new Intent(Intent.ACTION_PICK);
//                intent.setType("image/*");
//                startActivityForResult(intent, 0);
                break;
        }
    }

    @Override
    protected void onActivityResult(int reqCode, int resCode, Intent data) {
        if (reqCode == 0 && resCode == RESULT_OK) {
            Uri uri = data.getData();
//            mImageView.setImageURI(uri);
            ContentResolver cv = getContentResolver();
            try {
                InputStream is = cv.openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                mImageView.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void doFrame(long frameTimeNanos) {
//        ++frameNum;
//        Choreographer.getInstance().postFrameCallback(this);
    }

}