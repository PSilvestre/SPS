package com.example.sps.data_collection;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;

import com.example.sps.ScanBroadcastReceiver;

import java.io.FileWriter;
import java.util.List;

public class DataCollectOnClickListener implements View.OnClickListener {

    private String path;
    private TextView txt;
    private WifiManager manager;
    private boolean scanResult;
    private Integer counter;

    public DataCollectOnClickListener(String path, TextView txt, WifiManager manager, Activity src, Integer counter){
        this.manager = manager;
        this.path = path;
        this.txt = txt;
        this.counter = counter;

        BroadcastReceiver br = new CellScanBroadcastReceiver(this.path);
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        src.registerReceiver(br, filter);
    }

    @Override
    public void onClick(View v) {
        scanResult = this.manager.startScan();
    }

    public class CellScanBroadcastReceiver extends BroadcastReceiver {

        String path;

        public CellScanBroadcastReceiver(String path) {
            this.path = path;
        }

        @Override
        public void onReceive(Context context, Intent intent) { //can't change the args while overwriting
            List<ScanResult> results = manager.getScanResults();
            if(scanResult == true){
                txt.setText("Scan Ok " + ++counter + "/10");
            }  else {
                txt.setText("Scan Failed " + counter + "/10");
            }
            try {
                FileWriter fw = new FileWriter(Environment.getExternalStorageDirectory().getAbsolutePath() + path, true);

                for (ScanResult scanResult : results) {
                    fw.append(scanResult.BSSID + ", " + scanResult.level + "\n");
                }
                fw.append("\n");
                fw.flush();
                fw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
