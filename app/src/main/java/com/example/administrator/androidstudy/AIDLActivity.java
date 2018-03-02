package com.example.administrator.androidstudy;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.administrator.androidstudy.aidl.IMyAidlInterface;
import com.example.administrator.androidstudy.service.AIDLService;

/**
 * Created by Administrator on 2018/3/1.
 */

public class AIDLActivity extends AppCompatActivity {
    private IMyAidlInterface mService;

    @Override
    public void onCreate(@Nullable Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);
        setContentView(R.layout.layout);

        Log.v("AAAA:", ""+android.os.Process.myPid());
        Intent intent = new Intent(this, AIDLService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService  =  IMyAidlInterface.Stub.asInterface(service);
            try {
                Log.v("AAAA:", mService.add(1, 5)+"");
            } catch (RemoteException e) {

            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
}
