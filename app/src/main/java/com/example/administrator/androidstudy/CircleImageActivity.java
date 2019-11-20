package com.example.administrator.androidstudy;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import com.example.administrator.androidstudy.views.ClipCircleImageView;

/**
 * Created by ljm on 2018/3/27.
 */
public class CircleImageActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle onSavedInstanceState) {
        super.onCreate(onSavedInstanceState);
        setContentView(R.layout.layout_circle_image_view);
        ClipCircleImageView circleImageView = $(R.id.clip_image);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ss0);
        circleImageView.setImageBitmap(bitmap);
    }
}
