package com.example.sps.activity_recognizer;

import com.example.sps.Utils;
import com.example.sps.database.DatabaseService;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.sps.LocateMeActivity.ACCELEROMETER_SAMPLES_PER_SECOND;
import static com.example.sps.LocateMeActivity.NUM_ACC_READINGS;

public class FourierTransformActivityRecognizer implements ActivityRecognizer {


    private int dominantFrequencyIndex;
    private float dominantFrequencyHz;
    private float dominantFrequencyMagnitude;

    private SubjectActivity lastState = SubjectActivity.STANDING;


    @Override
    public SubjectActivity recognizeActivity(Queue<Float> sensorData, Queue<FloatTriplet> sensorDataRaw, DatabaseService dBconnection) {

        List<Float> mostRecent = new ArrayList<>(sensorData).subList(sensorData.size()/4 * 3, sensorData.size());

        float rawmean = Utils.mean(mostRecent);
        float rawstdDev = Utils.stdDeviation(mostRecent, rawmean);

        if (rawstdDev < 0.5) { lastState = SubjectActivity.STANDING; return SubjectActivity.STANDING;};

        List<Float> accelerometerDataMagnitudeFFT = Utils.fourierTransform(new ArrayList<>(sensorData));



        List<Float> ofInterest = accelerometerDataMagnitudeFFT.subList(1, accelerometerDataMagnitudeFFT.size() / 6);

        float mean = Utils.mean(ofInterest);
        float stddev = Utils.stdDeviation(ofInterest, mean);



        int dominantFrequencyIndex = Utils.argMax(ofInterest) + 1;
        float dominantFrequencyHz = dominantFrequencyIndex*ACCELEROMETER_SAMPLES_PER_SECOND/ ((float)NUM_ACC_READINGS);
        float dominantFrequencyMagnitude = accelerometerDataMagnitudeFFT.get(dominantFrequencyIndex);

        if(dominantFrequencyMagnitude > mean +3*stddev && dominantFrequencyHz > 0.7 && dominantFrequencyHz < 2.3) {
            this.dominantFrequencyIndex = dominantFrequencyIndex;
            this.dominantFrequencyHz = dominantFrequencyHz;
            this.dominantFrequencyMagnitude = dominantFrequencyMagnitude;
            lastState = SubjectActivity.WALKING;
            return SubjectActivity.WALKING;}


        return lastState;
    }



    @Override
    public int getSteps(Queue<Float> sensorData, Queue<FloatTriplet> sensorDataRaw, DatabaseService dBconnection, SubjectActivity currentActivityState, AtomicInteger accReadingsSinceLastUpdate) {
        if(currentActivityState == SubjectActivity.WALKING){
            int numUpdates = accReadingsSinceLastUpdate.get();
            int numSteps = (int) (numUpdates / (ACCELEROMETER_SAMPLES_PER_SECOND /  dominantFrequencyHz));

            int numUpdatesDecrease = (int) (- numSteps * ( (ACCELEROMETER_SAMPLES_PER_SECOND /  dominantFrequencyHz)));
            System.out.println("NUM_UPDATES: " + numUpdates + "\t numSteps:  " + numSteps + "\t numUpdatesDecrease: " + numUpdatesDecrease + "\tDominant Frq: " + dominantFrequencyHz + "\t Mag: " + dominantFrequencyMagnitude);

            accReadingsSinceLastUpdate.addAndGet(numUpdatesDecrease);
            return numSteps;

        }
        accReadingsSinceLastUpdate.set(0);
        return 0;
    }



}
