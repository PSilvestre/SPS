package com.example.sps;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import com.example.sps.activity_recognizer.FloatTriplet;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.sps.LocateMeActivity.NUM_ACC_READINGS;

public class AccelerometerListener implements SensorEventListener {

    Queue<FloatTriplet> toPopulate;
    AtomicInteger accReadingsSinceLastUpdate;

    public AccelerometerListener(Queue<FloatTriplet> toPopulate, AtomicInteger accReadingsSinceLastUpdate) {
        this.toPopulate = toPopulate;
        this.accReadingsSinceLastUpdate = accReadingsSinceLastUpdate;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
            if(toPopulate.size() >= NUM_ACC_READINGS)
                toPopulate.poll();
            toPopulate.add(new FloatTriplet(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]));
            if(accReadingsSinceLastUpdate != null)
                accReadingsSinceLastUpdate.incrementAndGet();
        }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
