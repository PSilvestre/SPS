package com.example.sps.activity_recognizer;

import com.example.sps.Utils;
import com.example.sps.database.DatabaseService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.sps.LocateMeActivity.NUM_ACC_READINGS;

//TODO we are using only z axis right now! we can do for all three and check periodicities of each one!
class AutocorrActivityRecognizer implements ActivityRecognizer {


    public static final int MIN_DELAY = 40;
    public static final int MAX_DELAY = 100;


    int minDelay = MIN_DELAY;
    int maxDelay = MAX_DELAY;

    int optDelay = 0;


    private SubjectActivity lastState = SubjectActivity.STANDING;

    @Override
    public SubjectActivity recognizeActivity(Queue<Float> sensorData, Queue<FloatTriplet> sensorDataRaw, DatabaseService dbconnection) {

        List<Float> sensorDataMagnitudeList = new ArrayList<>(sensorData);
        Collections.reverse(sensorDataMagnitudeList);

        List<Float> mostRecent = sensorDataMagnitudeList.subList(sensorDataMagnitudeList.size()/4 * 3, sensorDataMagnitudeList.size());

        float mean = Utils.mean(mostRecent);
        float stdDev = Utils.stdDeviation(mostRecent, mean);

        if (stdDev < 0.5) { lastState = SubjectActivity.STANDING; return SubjectActivity.STANDING;};
        if (stdDev > 2) {  return SubjectActivity.JERKY_MOTION;}

        if (optDelay != 0) {
            minDelay = Math.max(optDelay - 10, MIN_DELAY);
            maxDelay = Math.min(optDelay + 10, MAX_DELAY);
        }
        List<Float> correlationsForEachDelay = Utils.correlation(sensorDataMagnitudeList, sensorDataMagnitudeList, minDelay, maxDelay);


        int largestCorrelationIndex = Utils.argMax(correlationsForEachDelay);
        float correlation = correlationsForEachDelay.get(largestCorrelationIndex);

        if (correlation > 0.7  ) {
            if (optDelay == 0)
                optDelay = largestCorrelationIndex + minDelay;
            else
                optDelay = (int) (0.5 * optDelay + 0.5 * (largestCorrelationIndex + minDelay));

            lastState = SubjectActivity.WALKING;
            return SubjectActivity.WALKING;
        }
        return lastState;
    }

    @Override
    public int getSteps(Queue<Float> sensorData, Queue<FloatTriplet> sensorDataRaw, DatabaseService dBconnection, SubjectActivity currentActivityState, AtomicInteger accReadingsSinceLastUpdate) {
        if (currentActivityState == SubjectActivity.WALKING || (currentActivityState == SubjectActivity.JERKY_MOTION  && lastState == SubjectActivity.WALKING)) {
            int numUpdates = accReadingsSinceLastUpdate.get();
            int numSteps = numUpdates / (optDelay / 2);
            accReadingsSinceLastUpdate.addAndGet(-(optDelay / 2)*numSteps);
            return numSteps;
        }
        accReadingsSinceLastUpdate.set(0);
        return 0;
    }

}