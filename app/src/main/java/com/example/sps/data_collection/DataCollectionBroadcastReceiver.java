package com.example.sps.data_collection;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.List;

public class DataCollectionBroadcastReceiver extends BroadcastReceiver {

    private WifiManager wifiManager;
    private DataCollectionActivity src;

    public DataCollectionBroadcastReceiver(WifiManager wifiManager, DataCollectionActivity src) {
        this.wifiManager = wifiManager;
        this.src = src;

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        List<ScanResult> scanResults = wifiManager.getScanResults();

        if (src.getScanResults()) {
            try {
                File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/sps");
                if (!dir.exists())
                    dir.mkdir();


                FileWriter fw = new FileWriter(Environment.getExternalStorageDirectory().getAbsolutePath() + "/sps/" + src.getFileName(), true);

                for (ScanResult scanResult : scanResults) {
                    fw.append(scanResult.BSSID + ", " + scanResult.level + "\n");
                }
                fw.append("\n");
                fw.flush();
                fw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        src.incAndDisplayCounter();
    }
}
