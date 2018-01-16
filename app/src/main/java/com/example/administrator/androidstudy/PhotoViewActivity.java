package com.example.administrator.androidstudy;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.example.administrator.androidstudy.views.ClipCircleImageView;
import com.example.administrator.androidstudy.views.PhotoWallView;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by Administrator on 2018/1/10.
 */

public class PhotoViewActivity extends AppCompatActivity implements View.OnClickListener{
    private PhotoWallView mPhotoWallView;

    @Override
    public void onCreate(@Nullable Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);
        setContentView(R.layout.layout_photos_view);
        mPhotoWallView = findViewById(R.id.photo_view);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.init);
        mPhotoWallView.setInitBitmap(bitmap);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.save:

                break;
            case R.id.cancel:
                break;
            case R.id.select:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 0);
                break;
        }
    }

    @Override
    protected void onActivityResult(int reqCode, int resCode, Intent data) {
        if (reqCode == 0 && resCode == RESULT_OK) {
            Uri uri = data.getData();
            ContentResolver cv = getContentResolver();
            try {
                InputStream is = cv.openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

}
