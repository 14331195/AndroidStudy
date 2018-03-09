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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

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

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private Button cancel;
    private Button save;
    private Button select;
    private ClipCircleImageView mImageView;
    private ImageView mClipImageView;
    private int progress = 10;
    private MyProgressView myProgressView;

    @Override
    public void onCreate(@Nullable Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);
        setContentView(R.layout.layout);

        cancel = findViewById(R.id.cancel);
        select = findViewById(R.id.select);
        save = findViewById(R.id.save);
        mImageView = findViewById(R.id.clip_image);
        mClipImageView = findViewById(R.id.result);
        myProgressView = findViewById(R.id.my);
        mImageView.post(new Runnable() {
            @Override
            public void run() {
                mImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ss0));
            }
        });
        cancel.setOnClickListener(this);
        select.setOnClickListener(this);
        save.setOnClickListener(this);
        select.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent(MainActivity.this, AIDLActivity.class);
                startActivity(intent);
                return true;
            }
        });
        setupRecyclerView();
//        test();
    }

    private void test() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.dialog);
//        builder.setView(R.layout.img);

        View view = LayoutInflater.from(this).inflate(R.layout.img, null);
//        view.setLayoutParams(new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, 1000));

//        builder.show();
        AlertDialog dialog = builder.create();
//        Dialog dialog = new Dialog(this, R.style.dialog);
        dialog.setContentView(R.layout.img);
        dialog.show();

//        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
////        WindowManager.LayoutParams attrs = dialog.getWindow().getAttributes();
////        params.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
////        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
////        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
////        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
////        DisplayMetrics displayMetrics = new DisplayMetrics();
////        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
//        params.x = -20;
//        params.width = WindowManager.LayoutParams.MATCH_PARENT;
//        params.height = WindowManager.LayoutParams.MATCH_PARENT;
//        dialog.getWindow().setAttributes(params);
//        dialog.setContentView(R.layout.img);


    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        List<ItemAdapter.Item> list = new ArrayList<>();
        for (int i = 0; i < 20; ++i) {
            list.add(new ItemAdapter.Item(i));
        }
        ItemAdapter adapter = new ItemAdapter(this, list);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL));

        RecyclerView listView = findViewById(R.id.list_view);
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

}
