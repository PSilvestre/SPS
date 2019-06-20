package com.example.sps.activity_recognizer;

import com.example.sps.Utils;
import com.example.sps.database.DatabaseService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class StdDevActivityRecognizer implements ActivityRecognizer {

    @Override
    public SubjectActivity recognizeActivity(Queue<Float> sensorData, Queue<FloatTriplet> sensorDataRaw, DatabaseService dbconnection, AtomicInteger accReadingsSinceLastUpdate) {
        List<Float> sensorDataMagnitudeList = new ArrayList<>(sensorData);
        float mean = Utils.mean(sensorDataMagnitudeList);
        float stdDev = Utils.stdDeviation(sensorDataMagnitudeList, mean);


        if(stdDev > 3)
            return SubjectActivity.RUNNING;
        else if(stdDev > 0.8)
            return SubjectActivity.WALKING;
        else
            return SubjectActivity.STANDING;
    }

    @Override
    public int getSteps(Queue<Float> sensorData, Queue<FloatTriplet> sensorDataRaw, DatabaseService dBconnection, SubjectActivity currentActivityState, AtomicInteger accReadingsSinceLastUpdate) {

        if(currentActivityState == SubjectActivity.STANDING) {
            accReadingsSinceLastUpdate.set(0);
            return 0;
        }
        if(currentActivityState == SubjectActivity.WALKING ) {
            int numSteps = accReadingsSinceLastUpdate.get() / (55 / 2);
            int remainder = accReadingsSinceLastUpdate.get() % (55 / 2);

            accReadingsSinceLastUpdate.set(remainder);
            return numSteps;
        }
        if(currentActivityState == SubjectActivity.RUNNING) {
            int numSteps = accReadingsSinceLastUpdate.get() / (30 / 2);
            int remainder = accReadingsSinceLastUpdate.get() % (30 / 2);

            accReadingsSinceLastUpdate.set(remainder);
            return numSteps;
        }
        return 0;

    }

    public float[] calculateMeans(Queue<FloatTriplet> sensorData){
        float[] means = new float[3];

        for(FloatTriplet point : sensorData){
            means[0] += point.getX();
            means[1] += point.getY();
            means[2] += point.getZ();
        }

        for(int i = 0; i < 3; i++){
            means[i] /= sensorData.size();
        }
        return means;
    }

    public float[] calculateStddevs(Queue<FloatTriplet> sensorData, float[] means) {
        float[] stddevs = new float[3];

        for (FloatTriplet point : sensorData) {
            stddevs[0] += Math.pow(point.getX() - means[0], 2);
            stddevs[1] += Math.pow(point.getY() - means[1], 2);
            stddevs[2] += Math.pow(point.getZ() - means[2], 2);
        }

        for (int i = 0; i < 3; i++) {
            stddevs[i] = (float) Math.sqrt(stddevs[i] / sensorData.size());
        }
        return stddevs;
    }
}
