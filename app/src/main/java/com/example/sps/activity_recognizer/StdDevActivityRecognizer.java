package com.example.sps.activity_recognizer;

import java.util.List;

public class StdDevActivityRecognizer implements ActivityRecognizer {

    @Override
    public SubjectActivity recognizeActivity(List<FloatTriplet> sensorData) {

    }

    public float[] calculateMeans(List<FloatTriplet> sensorData){
        float[] means = new float[];

        for(FloatTriplet point : sensorData){
            means[0] += point.getX();
            means[1] += point.getY();
            means[2] += point.getZ();
        }

        for(int i = 0; i < 3; i++){
            means[i] /= sensorData.size();
        }
    }
}
