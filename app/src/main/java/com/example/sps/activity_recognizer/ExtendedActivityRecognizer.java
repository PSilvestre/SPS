package com.example.sps.activity_recognizer;

import com.example.sps.Utils;
import com.example.sps.database.DatabaseService;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.sps.LocateMeActivity.ACCELEROMETER_SAMPLES_PER_SECOND;


/* This activity is the same as AutocorrActivityRecognizer but it will correlate with many more
activities apart from just Walking.
RUNNING
STAIRS
ELEVATOR

It should return the one that autocorrelates the best or that correlates to a min threshold,
whatever happens first.
 */

class ExtendedActivityRecognizer implements ActivityRecognizer {


    private int optDelay;

    @Override
    public SubjectActivity recognizeActivity(Queue<FloatTriplet> sensorData, DatabaseService dbconnection) {

        SubjectActivity activity = SubjectActivity.STANDING;

        int minDelay = 40;
        int maxDelay = 100;

        List<Float> sensorDataMagnitudeList = new ArrayList<>();
        for(FloatTriplet f : sensorData) {
            float magnitude = (float) Math.sqrt(Math.pow(f.getX(), 2) + Math.pow(f.getY(), 2) + Math.pow(f.getZ(), 2));
            sensorDataMagnitudeList.add(magnitude);
        }

        List<List<FloatTriplet>> sensorDataListFromDatabase;

        List<SubjectActivity> activitiesToIdentify = new ArrayList<>();
        activitiesToIdentify.add(SubjectActivity.WALKING);
        activitiesToIdentify.add(SubjectActivity.RUNNING);
        activitiesToIdentify.add(SubjectActivity.STAIRS);
        activitiesToIdentify.add(SubjectActivity.ELEVATOR);


        for(int activityIndex = 0; activityIndex < activitiesToIdentify.size(); activityIndex ++) {
            sensorDataListFromDatabase = dbconnection.getActivityRecordings(activitiesToIdentify.get(activityIndex));
            if (sensorDataListFromDatabase == null) continue;

            for(List<FloatTriplet> recording : sensorDataListFromDatabase) {
                List<Float> dbDataMagnitudeList = new ArrayList<>();
                for (FloatTriplet f : recording) {
                    float magnitude = (float) Math.sqrt(Math.pow(f.getX(), 2) + Math.pow(f.getY(), 2) + Math.pow(f.getZ(), 2));
                    dbDataMagnitudeList.add(magnitude);
                }

                List<Float> correlationsForEachDelay = Utils.correlation(sensorDataMagnitudeList, dbDataMagnitudeList, minDelay, maxDelay);

                int largestCorrelationIndex = Utils.argMax(correlationsForEachDelay);
                float correlationMax = correlationsForEachDelay.get(largestCorrelationIndex);

                if (correlationMax > 0.8) {
                    optDelay = minDelay + largestCorrelationIndex;
                    return activitiesToIdentify.get(activityIndex);
                }
            }
        }

        return activity;

    }

    @Override
    public int getSteps(Queue<FloatTriplet> sensorData, DatabaseService dbconnection, SubjectActivity currentActivityState, AtomicInteger accReadingsSinceLastUpdate) {
        if (currentActivityState == SubjectActivity.WALKING || currentActivityState == SubjectActivity.RUNNING) {
            int numSteps = accReadingsSinceLastUpdate.get() / (optDelay / 2);
            int remainder = accReadingsSinceLastUpdate.get() % (optDelay / 2);
            System.out.println("ACC_READINGS_SINCE = " + accReadingsSinceLastUpdate.get() + "\tNUM STEPS = " + numSteps + "\tREMAINDER = " + remainder + "\t OPT DELAY = " + optDelay);

            accReadingsSinceLastUpdate.set(remainder);

            if (currentActivityState == SubjectActivity.RUNNING) {
                numSteps *= 2; // "Thomas Running invented running when he tried to walk twice" -> running = 2 * walk
            }
            return numSteps;
        }
        accReadingsSinceLastUpdate.set(0);
        return 0;
    }
}
