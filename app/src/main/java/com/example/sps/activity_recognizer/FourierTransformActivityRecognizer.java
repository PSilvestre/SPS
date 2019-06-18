package com.example.sps.activity_recognizer;

import com.example.sps.Utils;
import com.example.sps.database.DatabaseService;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class FourierTransformActivityRecognizer implements ActivityRecognizer {


    private int lastHz;
    private float lastMag;


    @Override
    public SubjectActivity recognizeActivity(Queue<Float> sensorData, Queue<FloatTriplet> sensorDataRaw, DatabaseService dBconnection) {
        List<Float> accelerometerDataMagnitudeFFT = Utils.fourierTransform(new ArrayList<>(sensorData));

        List<Float> except0 = accelerometerDataMagnitudeFFT.subList(1, accelerometerDataMagnitudeFFT.size());

        lastHz = Utils.argMax(except0) + 1;
        lastMag = accelerometerDataMagnitudeFFT.get(lastHz);
        if(lastHz == 7 && lastMag > 45) return SubjectActivity.WALKING;
        if(lastHz == 8 && lastMag > 45) return SubjectActivity.WALKING;
        if(lastHz == 9 && lastMag > 45) return SubjectActivity.WALKING;
        if(lastHz == 10 && lastMag > 45) return SubjectActivity.WALKING;
        if(lastHz == 11 && lastMag > 45) return SubjectActivity.WALKING;

        return SubjectActivity.STANDING;
    }



    @Override
    public int getSteps(Queue<Float> sensorData, Queue<FloatTriplet> sensorDataRaw, DatabaseService dBconnection, SubjectActivity currentActivityState, AtomicInteger accReadingsSinceLastUpdate) {
        if(currentActivityState == SubjectActivity.WALKING){
            if(lastHz == 8 && lastMag > 80 && accReadingsSinceLastUpdate.get() > 70) {accReadingsSinceLastUpdate.addAndGet(-70); return 1;}
            if(lastHz == 9 && lastMag > 80 && accReadingsSinceLastUpdate.get() > 60) {accReadingsSinceLastUpdate.addAndGet(-60); return 1;}
            if(lastHz == 10 && lastMag > 60 && accReadingsSinceLastUpdate.get() > 50) {accReadingsSinceLastUpdate.addAndGet(-50); return 1;}
            if(lastHz == 11 && lastMag > 65 && accReadingsSinceLastUpdate.get() > 40) {accReadingsSinceLastUpdate.addAndGet(-40); return 1;}
        }
        return 0;
    }



}
