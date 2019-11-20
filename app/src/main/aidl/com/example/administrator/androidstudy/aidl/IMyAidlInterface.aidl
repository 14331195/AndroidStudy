// IMyAidlInterface.aidl
package com.example.administrator.androidstudy.aidl;
import com.example.administrator.androidstudy.aidl.ICallback;

// Declare any non-default types here with import statements

interface IMyAidlInterface {
   int add(int a, int b);
   oneway void callback(ICallback l);
}
