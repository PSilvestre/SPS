package com.example.sps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ScanBroadcastReceiver extends BroadcastReceiver {

    MainActivity act;

    public ScanBroadcastReceiver(MainActivity act){
        super();
        this.act = act;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        act.setText();
        System.out.println("SCAN COMPLETED");
    }
}
