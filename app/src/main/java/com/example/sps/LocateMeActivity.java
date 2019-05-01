package com.example.sps;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.example.sps.activity_recognizer.ActivityRecognizer;
import com.example.sps.activity_recognizer.FloatTriplet;
import com.example.sps.activity_recognizer.SubjectActivity;
import com.example.sps.data_collection.DataCollectOnClickListener;
import com.example.sps.data_collection.DataCollectionActivity;
import com.example.sps.localization_method.LocalizationMethod;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LocateMeActivity extends AppCompatActivity implements SensorEventListener {

    private Button initialBeliefButton;
    private Button locateMeButton;
    private Button collectDataButton;

    private TextView cellText;
    private TextView actText;

    private ActivityRecognizer activityRecognizer;
    private LocalizationMethod localizationMethod;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private List<FloatTriplet> accSensor;

    List<Float> cellProbabilities;
    int numCells;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locate_me);

        initialBeliefButton = findViewById(R.id.btn_initial_belief);
        locateMeButton = findViewById(R.id.btn_locate_me);
        collectDataButton = findViewById(R.id.btn_collect_data);

        cellText = findViewById(R.id.cell_guess);
        actText = findViewById(R.id.act_guess);

        numCells = 4;

        initialBeliefButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setInitialBelief();
            }
        });

        locateMeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<ScanResult> scanResults = null;
                int cell_guess = localizationMethod.computeLocation(scanResults);
                accSensor = new LinkedList<>();

                // Set the sensor manager
                sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

                // if the default accelerometer exists
                if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
                    // set accelerometer
                    accelerometer = sensorManager
                            .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    // register 'this' as a listener that updates values. Each time a sensor value changes,
                    // the method 'onSensorChanged()' is called.
                    sensorManager.registerListener((SensorEventListener) view.getContext(), accelerometer,
                            SensorManager.SENSOR_DELAY_NORMAL);
                } else {
                    // No accelerometer!
                }

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

    @Override
    public void onSensorChanged(SensorEvent event) {
        // get the the x,y,z values of the accelerometer
        accSensor.add(new FloatTriplet(event.values[0], event.values[1], event.values[2]));
        if ( accSensor.size() == 10 ) {
            SubjectActivity activity = activityRecognizer.recognizeActivity(accSensor);
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //Do everything!!!
    }

    protected void setInitialBelief(){
        cellProbabilities = new ArrayList<>(numCells);
        for(int i = 0; i < numCells; i++)
            cellProbabilities.add(1.0f/numCells);
    }


}
