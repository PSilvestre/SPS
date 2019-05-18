package com.example.sps.data_collection;

import android.content.BroadcastReceiver;
import android.content.Context;
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
import com.example.sps.Utils;
import com.example.sps.data_loader.WifiScan;
import com.example.sps.database.DatabaseService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


public class DataCollectionActivity extends AppCompatActivity {

    /**
     * The wifi manager.
     */
    private WifiManager wifiManager;
    private ScanInfo scanInfo;

    private Button btnScan;
    private Button btnScan10;

    private TextView txtStatus;
    private TextView txtScans;
    private EditText txtFile;
    private BroadcastReceiver receiver;
    private IntentFilter filter;

    private AtomicInteger counter;

    private DatabaseService dbConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_collection);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        dbConnection = new DatabaseService(this);

        btnScan = (Button) findViewById(R.id.buttonScan);

        btnScan10 = (Button) findViewById(R.id.buttonScanx10);

        txtStatus = (TextView) findViewById(R.id.textStatus);
        txtScans = (TextView) findViewById(R.id.textScans);
        txtFile = (EditText) findViewById(R.id.textFile);
        receiver = new DataCollectionBroadcastReceiver(wifiManager, this, dbConnection);
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
                //TODO Fix Direction (with android magnetometer)
                scanInfo = new ScanInfo(wifiManager.startScan(), Integer.parseInt(txtFile.getText().toString()), Direction.EAST);
                if (scanInfo.isScanSuccessful())
                    txtStatus.setText("Reading Status: Valid Scan");
                else
                    txtStatus.setText("Reading Status: INVALID Scan");
            }
        });

        btnScan10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < 10; i++) {
                            scanInfo = new ScanInfo(wifiManager.startScan(), Integer.parseInt(txtFile.getText().toString()), Direction.EAST);
                            if (scanInfo.isScanSuccessful())
                                txtStatus.setText("Reading Status: Valid Scan");
                            else
                                txtStatus.setText("Reading Status: INVALID Scan");
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


    private void updateGaussians(){

        for(int cellId = 1; cellId <= dbConnection.getNumberOfCells(); cellId++){
            List<WifiScan> scansOfCell = dbConnection.getScansOfCell(cellId);

            Map<String, Float> means = Utils.calculateMeans(scansOfCell);
            Map<String, Float> stddevs = Utils.calculateStdDevs(scansOfCell, means);

            for(String bssid : means.keySet())
                dbConnection.insertTableGaussian(cellId, bssid, means.get(bssid), stddevs.get(bssid));

        }
    }

    @Override
    public void onPause(){
        super.onPause();
        this.unregisterReceiver(receiver);
        updateGaussians();

    }

    @Override
    public void onResume(){
        super.onResume();
        this.registerReceiver(receiver, filter);
    }


    public ScanInfo getScanInfo() {
        return scanInfo;
    }

    public String getFileName() {
        return txtFile.getText().toString();
    }

    public void incAndDisplayCounter(){
        txtScans.setText("Number of Scans: "+ counter.addAndGet(1));
    }
}
