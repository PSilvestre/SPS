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

        List<FloatTriplet> sensorDataList = new ArrayList<>(sensorData);


        List<List<FloatTriplet>> sensorDataListFromDatabase;

        List<SubjectActivity> activitiesToIdentify = new ArrayList<>();
        activitiesToIdentify.add(SubjectActivity.STANDING);
        activitiesToIdentify.add(SubjectActivity.WALKING);

        for(SubjectActivity activityToIdenfity : activitiesToIdentify) {
            sensorDataListFromDatabase = dBconnection.getActivityRecordings(activityToIdenfity);
            for(List<FloatTriplet> recording : sensorDataListFromDatabase) {
                System.out.println("Recording.size: " + recording.size());
                List<FloatTriplet> correlation = Utils.correlation(sensorDataList, recording, minDelay, maxDelay);

                for(FloatTriplet correlationForDelay : correlation)
                    if (correlationForDelay.getZ() > 0.7 && correlationForDelay.getX() > 0.5 && correlationForDelay.getY() > 0.6) {
                        System.out.println("X: " + correlationForDelay.getX() + " Y: " + correlationForDelay.getY() + " Z: " + correlationForDelay.getZ() + "\n");
                        return SubjectActivity.WALKING;
                    }

            }

        }

        return activity;
    }
}
