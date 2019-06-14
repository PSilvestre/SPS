package com.example.sps.activity_recognizer;

import com.example.sps.Utils;
import com.example.sps.database.DatabaseService;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
//TODO we are using only z axis right now! we can do for all three and check periodicities of each one!
class AutocorrActivityRecognizer implements ActivityRecognizer {

    @Override
    public SubjectActivity recognizeActivity(Queue<FloatTriplet> sensorData, DatabaseService dbconnection) {
        ActivityRecognizer activityAlgorithm = new StdDevActivityRecognizer();
        SubjectActivity activity = activityAlgorithm.recognizeActivity(sensorData, dbconnection);

        if (activity == SubjectActivity.STANDING)
            return activity;

        activity = SubjectActivity.STD_NOT_IDLE;

        float avgStepPeriod = 0.72f / 1.4f;  //avg step size [m/step] / avg walking speed [m/s]
        int maxDelay = 100;
        int minDelay = 40;

        List<Float> sensorDataMagnitudeList = new ArrayList<>();
        for(FloatTriplet f : sensorData) {
            float magnitude = (float) Math.sqrt(Math.pow(f.getX(), 2) + Math.pow(f.getY(), 2) + Math.pow(f.getZ(), 2));
            sensorDataMagnitudeList.add(0, magnitude);
        }

        List<Float> correlationsForEachDelay = Utils.correlation(sensorDataMagnitudeList, sensorDataMagnitudeList, minDelay, maxDelay);
        int delay = minDelay;
        for (Float correlation : correlationsForEachDelay) {
            if (correlation > 0.7) {
                return SubjectActivity.WALKING;
            }
            delay++;
        }

        return activity;
    }

}
