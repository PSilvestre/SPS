package com.example.sps.data_collection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.sps.R;
import com.example.sps.data_loader.WifiReading;

import java.util.concurrent.atomic.AtomicInteger;

public class DataCollectionActivity extends AppCompatActivity {

    /**
     * The wifi manager.
     */
    private WifiManager wifiManager;
    private boolean scanResult;

    private Button btnScan;
    private Button btnScan10;

    private TextView txtStatus;
    private TextView txtScans;
    private EditText txtFile;
    private BroadcastReceiver receiver;
    private IntentFilter filter;

    private AtomicInteger counter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_collection);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        btnScan = (Button) findViewById(R.id.buttonScan);

        btnScan10 = (Button) findViewById(R.id.buttonScanx10);

        txtStatus = (TextView) findViewById(R.id.textStatus);
        txtScans = (TextView) findViewById(R.id.textScans);
        txtFile = (EditText) findViewById(R.id.textFile);
        receiver = new DataCollectionBroadcastReceiver(wifiManager, this);
        filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        this.registerReceiver(receiver, filter);

        counter = new AtomicInteger(0);
        txtFile.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                counter.set(0);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanResult = wifiManager.startScan();
                if (scanResult)
                    txtStatus.setText("Valid Scan");
                else
                    txtStatus.setText("NOT Valid Scan");
            }
        });

        btnScan10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < 10; i++) {
                            scanResult = wifiManager.startScan();
                            if (scanResult)
                                txtStatus.setText("Valid Scan");
                            else
                                txtStatus.setText("NOT Valid Scan");
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        });





    }


    @Override
    public void onPause(){
        super.onPause();
        this.unregisterReceiver(receiver);
    }

    @Override
    public void onResume(){
        super.onResume();
        this.registerReceiver(receiver, filter);
    }


    public boolean getScanResults() {
        return scanResult;
    }

    public String getFileName() {
        return txtFile.getText().toString();
    }

    public void incAndDisplayCounter(){
        txtScans.setText("kkkkkkkkkkkk"+ counter.addAndGet(1));
    }
}
