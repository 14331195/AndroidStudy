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
import android.widget.Toast;

import com.example.administrator.androidstudy.aidl.ICallback;
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

        Log.v("AAAA onCreate:", ""+android.os.Process.myPid()+" thread:"+android.os.Process.myTid());
        Intent intent = new Intent(this, AIDLService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);


    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService  =  IMyAidlInterface.Stub.asInterface(service);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.v("AAA Thread:", ""+android.os.Process.myPid()+" thread:"+android.os.Process.myTid());
                        try {
                        mService.callback(new ICallback.Stub() {
                            @Override
                            public void call(int data) throws RemoteException {
//                        Toast.makeText(AIDLActivity.this, "aaaa:"+data, Toast.LENGTH_SHORT).show();
                                Log.v("AAAA onSer call:", ""+android.os.Process.myPid()+" thread:"+android.os.Process.myTid());
                                mService.add(1,2);
                            }

                            @Override
                            public IBinder asBinder() {
                                return this;
                            }
                        });
                        } catch (RemoteException e) {
                            Log.v("AAA:", "eee");
                        }
                    }
                }).start();



        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private void run() {
        if (mService != null) {
            try {
                mService.callback(new ICallback.Stub() {
                    @Override
                    public void call(int data) throws RemoteException {
                        Log.v("AAAA call:", "" + android.os.Process.myPid() + " thread:" + android.os.Process.myTid());
                    }

                    @Override
                    public IBinder asBinder() {
                        return null;
                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
