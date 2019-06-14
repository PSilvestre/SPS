package com.example.sps.activity_recognizer;

import com.example.sps.Utils;
import com.example.sps.database.DatabaseService;

import org.apache.commons.math3.stat.descriptive.moment.Mean;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.LinkedList;
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

        activity = SubjectActivity.FUCK;

        float avgStepPeriod = 0.72f / 1.4f;  //avg step size [m/step] / avg walking speed [m/s]
        int maxDelay = 100;
        int minDelay = 40;

        List<FloatTriplet> asensorDataList = new ArrayList<>(sensorData);
        List<FloatTriplet> sensorDataList = new ArrayList<>();
        for(FloatTriplet f : asensorDataList)
            sensorDataList.add(0,f);


        List<FloatTriplet> correlationsForEachDelay = Utils.correlation(sensorDataList, sensorDataList, minDelay, maxDelay);
        int delay = minDelay;
        for (FloatTriplet i : correlationsForEachDelay) {
            if (i.getZ() > 0.7 && i.getX() > 0.5 && i.getY() > 0.6) {
                System.out.println("DELAY:" + delay);
                System.out.println("X: " + i.getX() + " Y: " + i.getY() + " Z: " + i.getZ() + "\n");
                return SubjectActivity.WALKING;
            }
            delay++;
        }

        return activity;
    }

}
