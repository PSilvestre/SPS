package com.example.sps.activity_recognizer;

import com.example.sps.Utils;
import com.example.sps.database.DatabaseService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.sps.LocateMeActivity.ACCELEROMETER_SAMPLES_PER_SECOND;
import static com.example.sps.LocateMeActivity.NUM_ACC_READINGS;

public class AutocorrActivityRecognizer implements ActivityRecognizer {


    public static final int MIN_DELAY = 48;
    public static final int MAX_DELAY = 86;
    private static final float WALKING_THRESHOLD = 0.75f;
    private static final float STANDING_THRESHOLD = 0.40f;


    int minDelay = MIN_DELAY;
    int maxDelay = MAX_DELAY;

    int optDelay = 0;
    int lastOptDelay = 0;
    private SubjectActivity lastState = SubjectActivity.STANDING;
    private int samplesSinceTransition = 0;
    @Override
    public SubjectActivity recognizeActivity(Queue<Float> sensorData, Queue<FloatTriplet> sensorDataRaw, DatabaseService dbconnection, AtomicInteger accReadingsSinceLastUpdate) {

        List<Float> sensorDataMagnitudeList = new ArrayList<>(sensorData);

        List<Float> olderData = sensorDataMagnitudeList.subList(0, sensorDataMagnitudeList.size()/4 * 1);

        float mean = Utils.mean(olderData);
        float stdDev = Utils.stdDeviation(olderData, mean);

        if (stdDev < STANDING_THRESHOLD) { // 0.40
            minDelay = MIN_DELAY;
            maxDelay = MAX_DELAY;
            if (lastState == SubjectActivity.WALKING)
                samplesSinceTransition = accReadingsSinceLastUpdate.get();
            lastState = SubjectActivity.STANDING;
            optDelay = 0;
            accReadingsSinceLastUpdate.set(0);
            return SubjectActivity.STANDING;
        }

        if (optDelay != 0) {
            minDelay = Math.max(optDelay - 10, MIN_DELAY);
            maxDelay = Math.min(optDelay + 10, MAX_DELAY);
        }

        List<Float> correlationsForEachDelay = Utils.correlation(sensorDataMagnitudeList, sensorDataMagnitudeList, minDelay, maxDelay);


        int largestCorrelationIndex = Utils.argMax(correlationsForEachDelay);
        float correlation = correlationsForEachDelay.get(largestCorrelationIndex);


        if (correlation > WALKING_THRESHOLD) { // 0.75
            optDelay = largestCorrelationIndex + minDelay;
            lastOptDelay = optDelay;
            lastState = SubjectActivity.WALKING;
            return SubjectActivity.WALKING;
        }
        return lastState;
    }

    @Override
    public int getSteps(Queue<Float> sensorData, Queue<FloatTriplet> sensorDataRaw, DatabaseService dBconnection, SubjectActivity currentActivityState, AtomicInteger accReadingsSinceLastUpdate) {
        if (currentActivityState == SubjectActivity.WALKING) {
            int numUpdates = accReadingsSinceLastUpdate.get();
            int numSteps = numUpdates / (optDelay / 2);
            accReadingsSinceLastUpdate.addAndGet(-(optDelay / 2) * numSteps);
            return numSteps;
        }

        if (samplesSinceTransition != 0) {
            int numSteps = samplesSinceTransition / (lastOptDelay / 2);
            samplesSinceTransition = 0;
            accReadingsSinceLastUpdate.set(0);
            return numSteps;
        }

        accReadingsSinceLastUpdate.set(0);

        return 0;
    }

}