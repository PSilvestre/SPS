package com.example.sps.activity_recognizer;

import com.example.sps.Utils;
import com.example.sps.database.DatabaseService;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;


/* This activity is the same as AutocorrActivityRecognizer but it will correlate with many more
activities apart from just Walking.
RUNNING
STAIRS
ELEVATOR

It should return the one that autocorrelates the best or that correlates to a min threshold,
whatever happens first.
 */

class ExtendedActivityRecognizer implements ActivityRecognizer {


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


        for(SubjectActivity activityToIdenfity : activitiesToIdentify) {
            sensorDataListFromDatabase = dbconnection.getActivityRecordings(activityToIdenfity);
            if (sensorDataListFromDatabase.size() == 0) continue;

            for(List<FloatTriplet> recording : sensorDataListFromDatabase) {
                List<Float> dbDataMagnitudeList = new ArrayList<>();
                for(FloatTriplet f : recording) {
                    float magnitude = (float) Math.sqrt(Math.pow(f.getX(), 2) + Math.pow(f.getY(), 2) + Math.pow(f.getZ(), 2));
                    dbDataMagnitudeList.add(magnitude);
                }

                List<Float> correlation = Utils.correlation(sensorDataMagnitudeList, dbDataMagnitudeList, minDelay, maxDelay);

                for(Float correlationForDelay : correlation)
                    if (correlationForDelay > 0.7) {
                        return activityToIdenfity;
                    }

            }

        }

        return activity;

    }

    @Override
    public int getSteps(Queue<FloatTriplet> sensorData, DatabaseService dBconnection, SubjectActivity currentActivityState, AtomicInteger accReadingsSinceLastUpdate) {
        return 0;
    }
}
