package com.example.sps.data_collection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.sps.AccelerometerListener;
import com.example.sps.R;
import com.example.sps.Utils;
import com.example.sps.activity_recognizer.FloatTriplet;
import com.example.sps.activity_recognizer.SubjectActivity;
import com.example.sps.data_loader.WifiScan;
import com.example.sps.database.DatabaseService;
import com.example.sps.localization_method.LocalizationAlgorithm;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.sps.LocateMeActivity.NUM_ACC_READINGS;


public class DataCollectionActivity extends AppCompatActivity {

    /**
     * The wifi manager.
     */
    private WifiManager wifiManager;
    private SensorManager sensorManager;
    private Sensor accelemoter;
    private ScanInfo scanInfo;

    private Button btnScan;
    private Button btnScan10;
    private Button btnDeleteData;

    private Button btnRecordActivity;
    private Button btnDeleteActivityData;
    private Spinner actSpin;

    private TextView txtStatus;
    private TextView txtScans;
    private EditText txtFile;
    private BroadcastReceiver receiver;
    private IntentFilter filter;

    private AtomicInteger counter;

    private DatabaseService dbConnection;

    private SubjectActivity selectedActivity;

    private int updateGaussians;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_collection);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        updateGaussians = 0;

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelemoter = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        dbConnection = new DatabaseService(this);

        btnScan = (Button) findViewById(R.id.buttonScan);
        btnScan10 = (Button) findViewById(R.id.buttonScanx10);
        btnDeleteData = (Button) findViewById(R.id.deleteData);
        btnRecordActivity = (Button) findViewById(R.id.activityRecorderButton);
        btnDeleteActivityData = (Button) findViewById(R.id.deleteActivityData);

        actSpin = (Spinner) findViewById(R.id.activity_detection_spin);


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
                updateGaussians = 1;
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
                            while(scanInfo != null){
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            }
                        }
                    }
                }).start();
                updateGaussians = 1;
            }
        });

        btnDeleteData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dbConnection.deleteScanData();
            }
        });

        btnDeleteActivityData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dbConnection.deleteActivityData();
            }
        });

        ArrayAdapter<SubjectActivity> adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, SubjectActivity.values());
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        actSpin.setAdapter(adapter);
        selectedActivity = SubjectActivity.STANDING;
        //Set Listener for Localization Spinner changes
        actSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedActivity = ((SubjectActivity) adapterView.getItemAtPosition(i));

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        btnRecordActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Queue < FloatTriplet > data = new LinkedList<>();
                        SensorEventListener listener = new AccelerometerListener(data);
                        sensorManager.registerListener(listener, accelemoter, 50000);
                        while(data.size() < NUM_ACC_READINGS ){
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        sensorManager.unregisterListener(listener);

                        dbConnection.insertRecording( ((LinkedList<FloatTriplet>) data).subList(0, NUM_ACC_READINGS),selectedActivity);

                    }
                }).start();

            }
        });

    }


    private void updateGaussians(){

        dbConnection.clearGaussianTable();
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
        if (updateGaussians == 1)
            updateGaussians();
    }

    @Override
    public void onResume(){
        super.onResume();
        this.registerReceiver(receiver, filter);
        updateGaussians = 0;
    }


    public ScanInfo getScanInfo() {
        return scanInfo;
    }

    public void setScanInfoToNull() {
        this.scanInfo = null;
    }

    public String getFileName() {
        return txtFile.getText().toString();
    }

    public void incAndDisplayCounter(){
        txtScans.setText("Number of Scans: "+ counter.addAndGet(1));
    }
}
