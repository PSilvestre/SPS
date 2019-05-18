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
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.sps.activity_recognizer.ActivityAlgorithm;
import com.example.sps.activity_recognizer.ActivityRecognizer;
import com.example.sps.activity_recognizer.FloatTriplet;
import com.example.sps.activity_recognizer.SubjectActivity;
import com.example.sps.data_collection.DataCollectionActivity;
import com.example.sps.database.DatabaseService;
import com.example.sps.localization_method.KnnLocalizationMethod;
import com.example.sps.localization_method.LocalizationMethod;
import com.example.sps.localization_method.LocalizationAlgorithm;

import java.io.FileWriter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

//TODO: IMPLEMENT way of automatic finding statistics on measurements (auto measure save?)


//TODO: implement graphs of the different activities like "climbing the stairs", running
// walking with phone in pocket, walking with phone in the hand
//TODO: use another sensors to sense direction, like magnetometer/compass

//TODO (from old main):
//   - implement x ScansPerCell button to run all scans in one;
//   - implement just one broadcast catcher (like my example) BUT have it written to diff files;
//   - (if passing to the other file is needed, don't forget to carry the things that prevent crashing like pauses and resumes


//TODO: UI, duh


public class LocateMeActivity extends AppCompatActivity  {

    public static final int NUM_ACC_READINGS = 20;



    private Button initialBeliefButton;
    private Button locateMeButton;
    private Button collectDataButton;

    private TextView cellText;
    private TextView actText;
    private TextView miscText;

    private Spinner locSpin;
    private Spinner actSpin;

    private EditText currCellText;

    private ActivityRecognizer activityRecognizer;
    private LocalizationMethod localizationMethod;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private CopyOnWriteArrayList<FloatTriplet> accelerometerData;

    private List<ScanResult> scanData;

    private AccelerometerListener accelerometerListener;

    private IntentFilter wifiIntentFilter;
    private BroadcastReceiver wifiBroadcastReceiver;

    private WifiManager wifiManager;

    private float[] cellProbabilities;

    private DatabaseService databaseService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locate_me);

        databaseService = new DatabaseService(this);
        initialBeliefButton = findViewById(R.id.btn_initial_belief);
        locateMeButton = findViewById(R.id.btn_locate_me);
        collectDataButton = findViewById(R.id.btn_collect_data);

        cellText = findViewById(R.id.cell_guess);
        actText = findViewById(R.id.act_guess);
        miscText = findViewById(R.id.misc_info);
        currCellText = findViewById(R.id.currCell);

        locSpin = findViewById(R.id.localization_algorithm_spin);
        actSpin = findViewById(R.id.activity_detection_spin);


        //Set Adapter for the Localization Spinner
        ArrayAdapter<LocalizationAlgorithm> adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, LocalizationAlgorithm.values());
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        locSpin.setAdapter(adapter);
        //Set Listener for Localization Spinner changes
        locSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                localizationMethod = ((LocalizationAlgorithm)adapterView.getItemAtPosition(i)).getMethod();

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                localizationMethod = localizationMethod; //Do nothing..
            }
        });

        //Set Adapter for the Activity Spinner
        ArrayAdapter<ActivityAlgorithm> adapterAct = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, ActivityAlgorithm.values());
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        actSpin.setAdapter(adapterAct);
        //Set Listener for Activity Spinner changes
        actSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                activityRecognizer = ((ActivityAlgorithm)adapterView.getItemAtPosition(i)).getMethod();

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                activityRecognizer = activityRecognizer; //do nothing
            }
        });



        activityRecognizer = ActivityAlgorithm.NORMAL.getMethod();
        localizationMethod = LocalizationAlgorithm.KNN_RSSI.getMethod();

        accelerometerData = new CopyOnWriteArrayList<>();
        accelerometerListener = new AccelerometerListener(accelerometerData);

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
                cellText.setText("Loading...");
                actText.setText("");
                accelerometerData.removeAll(accelerometerData);
                if (scanData != null)
                    scanData.removeAll(scanData);

                // Set the sensor manager
                sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

                //Start wifi scan
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
                    System.out.println("No accelerometer\n");
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {


                        while (scanData == null || scanData.size() == 0 || accelerometerData.size() < NUM_ACC_READINGS) { //spin while data not ready
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        //when finished, compute location and activity and post to user. unregister accelorometer listener

                        sensorManager.unregisterListener(accelerometerListener);


                        final SubjectActivity activity = activityRecognizer.recognizeActivity(accelerometerData);
                        cellProbabilities = localizationMethod.computeLocation(scanData, cellProbabilities, databaseService);

                        final int cell = getIndexOfLargest(cellProbabilities) + 1;

                        if (! currCellText.getText().toString().equals("CurrentCell (for stats)")) {
                            int txtCell = Integer.parseInt(currCellText.getText().toString());

                            try {
                                FileWriter fw = new FileWriter(Environment.getExternalStorageDirectory().getAbsolutePath() + "/sps/stats.txt", true);
                                fw.append(txtCell + "," + cell + "," + Math.round(cellProbabilities[cell - 1] * 100) + "," + localizationMethod.getClass().getName() + "," + localizationMethod.getMiscInfo() + "\n");
                                fw.flush();
                                fw.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        final float confidence = cellProbabilities[cell - 1];
                        System.out.println("confidence is" + confidence);
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                // Stuff that updates the UI
                                setLocalizationText(activity, cell, confidence);
                            }
                        });
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

    public int getIndexOfLargest( float[] array ) {
        if ( array == null || array.length == 0 )
            return -1;

        int largest = 0;
        for ( int i = 1; i < array.length; i++ ) {
            if ( array[i] > array[largest] ) largest = i;
        }
        return largest;
    }




    private void setLocalizationText(SubjectActivity activity, int cell, float confidence) {
        this.actText.setText("You are " + activity.toString());
        this.cellText.setText("You are at cell " + cell + " with confidence " + Math.round((confidence*100)*100)/100 + "%");
        if(this.localizationMethod instanceof KnnLocalizationMethod)
            miscText.setText("Number of Neighbours: " + ((KnnLocalizationMethod) localizationMethod).getNumNeighbours());
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
        //UPDATE GAUSSIANS HERE
    }


    protected void setInitialBelief(){
        int numCells = databaseService.getNumberOfCells();
        cellProbabilities = new float[numCells];
        for(int i = 0; i < numCells; i++)
            cellProbabilities[i] = 1.0f/numCells;
    }

    public class simpleScanBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            scanData = wifiManager.getScanResults();
        }
    }



}
