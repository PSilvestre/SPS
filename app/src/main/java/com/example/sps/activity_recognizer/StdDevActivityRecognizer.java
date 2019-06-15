package com.example.sps.activity_recognizer;

import com.example.sps.database.DatabaseService;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class StdDevActivityRecognizer implements ActivityRecognizer {

    @Override
    public SubjectActivity recognizeActivity(Queue<FloatTriplet> sensorData, DatabaseService dbconnection) {
        float[] means =  calculateMeans(sensorData);
        float[] stddevs = calculateStddevs(sensorData, means);


        int largestComponentIndex = 0;
        for(int i = 0; i < 3; i++) {
            if (means[i] > means[largestComponentIndex])
                largestComponentIndex = i;
        }
        if(stddevs[largestComponentIndex] > 3)
            return SubjectActivity.RUNNING;
        else if(stddevs[largestComponentIndex] > 1)
            return SubjectActivity.WALKING;
        else
            return SubjectActivity.STANDING;
    }

    @Override
    public int getSteps(Queue<FloatTriplet> sensorData, DatabaseService dBconnection, SubjectActivity currentActivityState, AtomicInteger accReadingsSinceLastUpdate) {

        if(currentActivityState == SubjectActivity.STANDING) return 0;
        if(currentActivityState == SubjectActivity.WALKING ) return 1;
        if(currentActivityState == SubjectActivity.RUNNING) return 2;
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
