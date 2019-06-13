package com.example.sps.activity_recognizer;

import com.example.sps.Utils;

import org.apache.commons.math3.stat.descriptive.moment.Mean;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
//TODO we are using only z axis right now! we can do for all three and check periodicities of each one!
class AutocorrActivityRecognizer implements ActivityRecognizer {

    @Override
    public SubjectActivity recognizeActivity(Queue<FloatTriplet> sensorData) {
        ActivityRecognizer activityAlgorithm = new StdDevActivityRecognizer();
        SubjectActivity activity = activityAlgorithm.recognizeActivity(sensorData);

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


        List<FloatTriplet> array1 = new ArrayList<>();
        List<FloatTriplet> array2 = new ArrayList<>();

        FloatTriplet mean1, mean2;
        FloatTriplet stdDev1, stdDev2;


        List<FloatTriplet> correlationForEachDelay = new ArrayList<>();

        for (int delay = minDelay; delay < maxDelay; delay++) {
            FloatTriplet sum = new FloatTriplet(0, 0, 0);

            array1 = sensorDataList.subList(0, delay - 1);
            array2 = sensorDataList.subList(delay, 2 * delay - 1);
            mean1 = Utils.Mean(array1);
            mean2 = Utils.Mean(array2);
            stdDev1 = Utils.StdDeviation(array1, mean1);
            stdDev2 = Utils.StdDeviation(array2, mean2);
            float dotsum = 0;
            for (int k = 0; k < delay-1; k++) {
                sum.setX(sum.getX() + ((array1.get(k).getX() - mean1.getX()) * (array2.get(k).getX() - mean2.getX())));
                sum.setY(sum.getY() + ((array1.get(k).getY() - mean1.getY()) * (array2.get(k).getY() - mean2.getY())));
                sum.setZ(sum.getZ() + ((array1.get(k).getZ() - mean1.getZ()) * (array2.get(k).getZ() - mean2.getZ())));
            }
            sum.setX(sum.getX() / (delay * stdDev1.getX() * stdDev2.getX()));
            sum.setY(sum.getY() / (delay * stdDev1.getY() * stdDev2.getY()));
            sum.setZ(sum.getZ() / (delay * stdDev1.getZ() * stdDev2.getZ()));
            correlationForEachDelay.add(sum);

            if( sum.getZ() > 0.7 && sum.getX() > 0.5 && sum.getY() > 0.6 ) {
                System.out.println("DELAY:" + delay);
                System.out.println("X: " + sum.getX() + " Y: " + sum.getY() + " Z: " + sum.getZ() + "\n");
                return SubjectActivity.WALKING;
            }
        }

        return activity;
    }




}
