package com.example.sps.activity_recognizer;

import com.example.sps.Utils;
import com.example.sps.database.DatabaseService;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

//TODO we are using only z axis right now! we can do for all three and check periodicities of each one!
class AutocorrActivityRecognizer implements ActivityRecognizer {



    int minDelay = 40;
    int maxDelay = 100;

    int optDelay = 0;


    @Override
    public SubjectActivity recognizeActivity(Queue<FloatTriplet> sensorData, DatabaseService dbconnection) {
        ActivityRecognizer activityAlgorithm = new StdDevActivityRecognizer();
        SubjectActivity activity = activityAlgorithm.recognizeActivity(sensorData, dbconnection);

        if (activity == SubjectActivity.STANDING)
            return activity;

        activity = SubjectActivity.STD_NOT_IDLE;


        List<Float> sensorDataMagnitudeList = new ArrayList<>();
        for(FloatTriplet f : sensorData) {
            float magnitude = (float) Math.sqrt(Math.pow(f.getX(), 2) + Math.pow(f.getY(), 2) + Math.pow(f.getZ(), 2));
            sensorDataMagnitudeList.add(0, magnitude);
        }

        List<Float> correlationsForEachDelay = Utils.correlation(sensorDataMagnitudeList, sensorDataMagnitudeList, minDelay, maxDelay);
        int delayDetected = minDelay;

        for (Float correlation : correlationsForEachDelay) {
            if (correlation > 0.7) {
                if(optDelay == 0)
                    optDelay = delayDetected;
                else
                    optDelay = (int) (0.5 * optDelay + 0.5 * delayDetected);
                return SubjectActivity.WALKING;
            }
            delayDetected++;
        }

        return activity;
    }

    @Override
    public int getSteps(Queue<FloatTriplet> sensorData, DatabaseService dBconnection, SubjectActivity currentActivityState, AtomicInteger accReadingsSinceLastUpdate) {
        if(currentActivityState == SubjectActivity.WALKING) {
            int numSteps =  accReadingsSinceLastUpdate.get() / (optDelay / 2);
            int remainder = accReadingsSinceLastUpdate.get() % (optDelay / 2);
            System.out.println("ACC_READINGS_SINCE = " + accReadingsSinceLastUpdate.get() + "\tNUM STEPS = " + numSteps + "\tREMAINDER = " + remainder + "\t OPT DELAY = " + optDelay);

            accReadingsSinceLastUpdate.set(remainder);
            return numSteps;
        }
        accReadingsSinceLastUpdate.set(0);
        return 0;
    }

}
