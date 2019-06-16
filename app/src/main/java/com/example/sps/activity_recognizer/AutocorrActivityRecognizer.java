package com.example.sps.activity_recognizer;

import com.example.sps.Utils;
import com.example.sps.database.DatabaseService;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

//TODO we are using only z axis right now! we can do for all three and check periodicities of each one!
class AutocorrActivityRecognizer implements ActivityRecognizer {


    public static final int MIN_DELAY = 45;
    public static final int MAX_DELAY = 75;


    int minDelay = MIN_DELAY;
    int maxDelay = MAX_DELAY;

    int optDelay = 0;


    @Override
    public SubjectActivity recognizeActivity(Queue<FloatTriplet> sensorData, DatabaseService dbconnection) {

        List<Float> sensorDataMagnitudeList = new ArrayList<>();
        for (FloatTriplet f : sensorData) {
            float magnitude = (float) Math.sqrt(Math.pow(f.getX(), 2) + Math.pow(f.getY(), 2) + Math.pow(f.getZ(), 2));
            sensorDataMagnitudeList.add(0, magnitude);
        }

        float mean = Utils.mean(sensorDataMagnitudeList);
        float stdDev = Utils.stdDeviation(sensorDataMagnitudeList, mean);

        if (stdDev < 0.6) return SubjectActivity.STANDING;

        if (optDelay != 0) {
            minDelay = Math.max(optDelay - 10, MIN_DELAY);
            maxDelay = Math.min(optDelay + 10, MAX_DELAY);
        }
        List<Float> correlationsForEachDelay = Utils.correlation(sensorDataMagnitudeList, sensorDataMagnitudeList, minDelay, maxDelay);
        int delayDetected = minDelay;


        int largestCorrelationIndex = Utils.argMax(correlationsForEachDelay);
        float correlation = correlationsForEachDelay.get(largestCorrelationIndex);

        if (correlation > 0.8) {
            if (optDelay == 0)
                optDelay = largestCorrelationIndex + minDelay;
            else
                optDelay = (int) (0.5 * optDelay + 0.5 * (largestCorrelationIndex + minDelay));
            return SubjectActivity.WALKING;
        }

        return SubjectActivity.STD_NOT_IDLE;
    }

    @Override
    public int getSteps(Queue<FloatTriplet> sensorData, DatabaseService dBconnection, SubjectActivity currentActivityState, AtomicInteger accReadingsSinceLastUpdate) {
        if (currentActivityState == SubjectActivity.WALKING) {
            int numSteps = accReadingsSinceLastUpdate.get() / (optDelay / 2);
            int remainder = accReadingsSinceLastUpdate.get() % (optDelay / 2);

            accReadingsSinceLastUpdate.set(remainder);
            return numSteps;
        }
        accReadingsSinceLastUpdate.set(0);
        return 0;
    }

}
