package com.example.sps;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.sps.activity_recognizer.ActivityRecognizer;
import com.example.sps.activity_recognizer.FloatTriplet;
import com.example.sps.activity_recognizer.SubjectActivity;
import com.example.sps.data_collection.DataCollectionActivity;
import com.example.sps.localization_method.LocalizationMethod;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LocateMeActivity extends AppCompatActivity  {
    public static final int NUM_CELLS = 4;

    private static final int NUM_ACC_READINGS = 20;


    private Button initialBeliefButton;
    private Button locateMeButton;
    private Button collectDataButton;

    private TextView cellText;
    private TextView actText;

    private ActivityRecognizer activityRecognizer;
    private LocalizationMethod localizationMethod;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private List<FloatTriplet> accelorometerData;
    private List<ScanResult> scanData;

    private AccelerometerListener accelerometerListener;

    private IntentFilter wifiIntentFilter;
    private BroadcastReceiver wifiBroadcastReceiver;

    private WifiManager wifiManager;

    private List<Float> cellProbabilities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locate_me);

        initialBeliefButton = findViewById(R.id.btn_initial_belief);
        locateMeButton = findViewById(R.id.btn_locate_me);
        collectDataButton = findViewById(R.id.btn_collect_data);

        cellText = findViewById(R.id.cell_guess);
        actText = findViewById(R.id.act_guess);


        accelorometerData = new LinkedList<>();
        accelerometerListener = new AccelerometerListener(accelorometerData);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        wifiBroadcastReceiver = new simpleScanBroadcastReceiver();
        wifiIntentFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        this.registerReceiver(wifiBroadcastReceiver, wifiIntentFilter);

        initialBeliefButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setInitialBelief();
            }
        });

        locateMeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                accelorometerData = new LinkedList<>();

                // Set the sensor manager
                sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

                wifiManager.startScan();
                // if the default accelerometer exists
                if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
                    // set accelerometer
                    accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    // register 'this' as a listener that updates values. Each time a sensor value changes,
                    // the method 'onSensorChanged()' is called.
                    sensorManager.registerListener(accelerometerListener, accelerometer,
                            SensorManager.SENSOR_DELAY_NORMAL);
                } else {
                    // No accelerometer!
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while( scanData == null || scanData.size() == 0  || accelorometerData.size() < NUM_ACC_READINGS){ //spin while data not ready
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        //when finished, compute location and activity and post to user. unregister accelorometer listener

                        sensorManager.unregisterListener(accelerometerListener);

                        accelorometerData = accelorometerData.subList(0, NUM_ACC_READINGS);

                        SubjectActivity activity = activityRecognizer.recognizeActivity(accelorometerData);
                        int cell = localizationMethod.computeLocation(scanData);

                        setLocalizationText(activity, cell);


                    }
                }).start();


            }
        });

        collectDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent((Activity) view.getContext(), DataCollectionActivity.class);
                startActivity(intent);
            }
        });

    }

    private void setLocalizationText(SubjectActivity activity, int cell) {
        this.actText.setText(R.string.act_guess_s + activity.name());
        this.cellText.setText(R.string.cell_guess_s + cell);
    }

    @Override
    public void onPause(){
        super.onPause();
        this.unregisterReceiver(wifiBroadcastReceiver);
    }

    @Override
    public void onResume(){
        super.onResume();
        this.registerReceiver(wifiBroadcastReceiver, wifiIntentFilter);
    }

    protected void setInitialBelief(){
        cellProbabilities = new ArrayList<>(NUM_CELLS);
        for(int i = 0; i < NUM_CELLS; i++)
            cellProbabilities.add(1.0f/NUM_CELLS);
    }

    public class simpleScanBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            scanData = wifiManager.getScanResults();
        }
    }


}
