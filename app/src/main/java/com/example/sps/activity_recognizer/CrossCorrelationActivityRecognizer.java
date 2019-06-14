package com.example.sps.activity_recognizer;

import com.example.sps.Utils;
import com.example.sps.database.DatabaseService;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class CrossCorrelationActivityRecognizer implements ActivityRecognizer {



    @Override
    public SubjectActivity recognizeActivity(Queue<FloatTriplet> sensorData, DatabaseService dBconnection) {
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

        for(SubjectActivity activityToIdenfity : activitiesToIdentify) {
            sensorDataListFromDatabase = dBconnection.getActivityRecordings(activityToIdenfity);
            for(List<FloatTriplet> recording : sensorDataListFromDatabase) {
                List<Float> dbDataMagnitudeList = new ArrayList<>();
                for(FloatTriplet f : recording) {
                    float magnitude = (float) Math.sqrt(Math.pow(f.getX(), 2) + Math.pow(f.getY(), 2) + Math.pow(f.getZ(), 2));
                    dbDataMagnitudeList.add(magnitude);
                }

                System.out.println("Recording.size: " + recording.size());
                List<Float> correlation = Utils.correlation(sensorDataMagnitudeList, dbDataMagnitudeList, minDelay, maxDelay);

                for(Float correlationForDelay : correlation)
                    if (correlationForDelay > 0.7) {
                        return SubjectActivity.WALKING;
                    }

            }

        }

        return activity;
    }
}
