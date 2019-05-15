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
import java.util.LinkedList;
import java.util.List;

public class DataCollectionBroadcastReceiver extends BroadcastReceiver {

    private WifiManager wifiManager;
    private DataCollectionActivity src;
    private DatabaseService db;

    public DataCollectionBroadcastReceiver(WifiManager wifiManager, DataCollectionActivity src, DatabaseService dbservice) {
        this.wifiManager = wifiManager;
        this.src = src;
        this.db = dbservice;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        List<ScanResult> scanResults = wifiManager.getScanResults();
        //TODO iterate and remove SSIDs with "AP"
        ScanInfo scanInfo = src.getScanInfo();
        if (scanInfo.isScanSuccessful()) {
            db.insertTableScan(scanInfo.getCellId(),scanInfo.getDirection(), scanResults);
        }

        src.incAndDisplayCounter();
    }
}
