package com.example.sps.data_collection;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;

import com.example.sps.database.DatabaseService;

import java.io.File;
import java.io.FileWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class DataCollectionBroadcastReceiver extends BroadcastReceiver {

    private WifiManager wifiManager;
    private DataCollectionActivity src;
    private DatabaseService db;

    private String[] bannedStrings = new String[]{"AP", "Android", "iPhone", "HotSpot", "Hotspot"};

    public DataCollectionBroadcastReceiver(WifiManager wifiManager, DataCollectionActivity src, DatabaseService dbservice) {
        this.wifiManager = wifiManager;
        this.src = src;
        this.db = dbservice;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        List<ScanResult> scanResults = wifiManager.getScanResults();
        ScanInfo scanInfo = src.getScanInfo();
        if (scanInfo.isScanSuccessful()) {
            List<ScanResult> filteredScanResults = new LinkedList<>();
            boolean containsBanned = false;
            for(ScanResult s : scanResults){
                for(String banned : bannedStrings) {
                    if (s.BSSID.contains(banned)) {
                        containsBanned = true;
                        break;
                    }
                }
                if(!containsBanned) filteredScanResults.add(s);
            }
            db.insertTableScan(scanInfo.getCellId(),scanInfo.getDirection(), filteredScanResults);
        }

        src.incAndDisplayCounter();
    }
}
