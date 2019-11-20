package com.example.administrator.androidstudy.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.example.administrator.androidstudy.aidl.ICallback;
import com.example.administrator.androidstudy.aidl.IMyAidlInterface;

/**
 * Created by Administrator on 2018/3/1.
 */

public class AIDLService extends Service {

    private IBinder aidlInterface = new IMyAidlInterface.Stub() {
        @Override
        public int add(int a, int b) throws RemoteException {
            Log.v("AAAA add:",  ""+android.os.Process.myPid()+" thread:"+android.os.Process.myTid());
            return a + b;
        }
        @Override
        public void callback(ICallback l) {
            Log.v("AAAA AIDLService:",  ""+android.os.Process.myPid()+" thread:"+android.os.Process.myTid());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.v("AAA Service Thread:",  ""+android.os.Process.myPid()+" thread:"+android.os.Process.myTid());


            try {
                l.call(9999);
            } catch (RemoteException e) {
                Log.v("AAA:", "eeeeeee");
                e.printStackTrace();
            }
                }
            }).start();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return aidlInterface;
    }

    @Override
    public void onCreate() {
        Log.v("AAAA Sevice onCreate:",  ""+android.os.Process.myPid()+" thread:"+android.os.Process.myTid());

    }


}


