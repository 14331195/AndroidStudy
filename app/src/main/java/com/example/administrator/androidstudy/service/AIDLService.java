package com.example.administrator.androidstudy.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.example.administrator.androidstudy.aidl.IMyAidlInterface;

/**
 * Created by Administrator on 2018/3/1.
 */

public class AIDLService extends Service {

    private IBinder aidlInterface = new IMyAidlInterface.Stub() {
        @Override
        public int add(int a, int b) throws RemoteException {
            Log.v("AAAA:",  ""+android.os.Process.myPid());
            return a + b;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return aidlInterface;
    }


}


