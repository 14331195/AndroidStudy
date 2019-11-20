package com.example.administrator.androidstudy;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

/**
 * Created by ljm on 2018/3/27.
 */
public class BaseActivity extends AppCompatActivity {

    protected <T extends View> T $(int id) {
        return (T)super.findViewById(id);
    }

    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }

    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

        handler.removeCallbacksAndMessages(null);
    }
}
