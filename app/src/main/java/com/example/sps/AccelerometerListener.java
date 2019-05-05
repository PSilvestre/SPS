package com.example.sps;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import com.example.sps.activity_recognizer.FloatTriplet;

import java.util.List;

import static com.example.sps.LocateMeActivity.NUM_ACC_READINGS;

public class AccelerometerListener implements SensorEventListener {

    List<FloatTriplet> toPopulate;

    public AccelerometerListener(List<FloatTriplet> toPopulate) {
        this.toPopulate = toPopulate;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(toPopulate.size() < NUM_ACC_READINGS)
            toPopulate.add(new FloatTriplet(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
