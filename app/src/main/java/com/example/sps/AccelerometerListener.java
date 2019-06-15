package com.example.sps;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import com.example.sps.activity_recognizer.FloatTriplet;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.sps.LocateMeActivity.NUM_ACC_READINGS;

public class AccelerometerListener implements SensorEventListener {

    private static final float ALPHA = 0.25f;

    Queue<FloatTriplet> toPopulate;
    AtomicInteger accReadingsSinceLastUpdate;
    FloatTriplet previousSample;

    List<Float> latestXs;
    List<Float> latestYs;
    List<Float> latestZs;

    public AccelerometerListener(Queue<FloatTriplet> toPopulate, AtomicInteger accReadingsSinceLastUpdate) {
        this.toPopulate = toPopulate;
        this.accReadingsSinceLastUpdate = accReadingsSinceLastUpdate;
        this.previousSample = null;

        latestXs = new ArrayList<>();
        latestYs = new ArrayList<>();
        latestZs = new ArrayList<>();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (toPopulate.size() >= NUM_ACC_READINGS)
            toPopulate.poll();

        FloatTriplet newFloatTriplet = new FloatTriplet(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);

        if (previousSample != null) {
            newFloatTriplet.setX(previousSample.getX() + ALPHA * (newFloatTriplet.getX() - previousSample.getX()));
            newFloatTriplet.setY(previousSample.getY() + ALPHA * (newFloatTriplet.getY() - previousSample.getY()));
            newFloatTriplet.setZ(previousSample.getZ() + ALPHA * (newFloatTriplet.getZ() - previousSample.getZ()));

        }


        toPopulate.add(newFloatTriplet);


        previousSample = newFloatTriplet;
        if (accReadingsSinceLastUpdate != null)
            accReadingsSinceLastUpdate.incrementAndGet();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
