package com.example.sps.activity_recognizer;

import com.example.sps.Utils;
import com.example.sps.database.DatabaseService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.sps.LocateMeActivity.ACCELEROMETER_SAMPLES_PER_SECOND;
import static com.example.sps.LocateMeActivity.NUM_ACC_READINGS;


/* This activity is the same as AutocorrActivityRecognizer but it will correlate with many more
activities apart from just Walking.
RUNNING
STAIRS
ELEVATOR

It should return the one that autocorrelates the best or that correlates to a min threshold,
whatever happens first.
 */

class CrossCorrActivityRecognizer implements ActivityRecognizer {



    @Override
    public SubjectActivity recognizeActivity(Queue<Float> sensorData, Queue<FloatTriplet> sensorDataRaw, DatabaseService dbconnection) {


        int minDelay = 40;
        int maxDelay = 100;

        List<Float> sensorDataMagnitudeList = new ArrayList<>(sensorData);

        float mean = Utils.mean(sensorDataMagnitudeList);
        float stdDev = Utils.stdDeviation(sensorDataMagnitudeList, mean);

        if (stdDev < 0.8) {return SubjectActivity.STANDING;}

        //Get the magnitude of the acceleration sensors
        List<Float> sensorDataX = new ArrayList<>();
        List<Float> sensorDataY = new ArrayList<>();
        List<Float> sensorDataZ = new ArrayList<>();

        for (FloatTriplet f : sensorDataRaw){
            sensorDataX.add(f.getX());
            sensorDataY.add(f.getY());
            sensorDataZ.add(f.getZ());
        }

        Map<SubjectActivity, Float> maxCorrelationPerActivity = new HashMap<>();

        /*
        //Compute FFT of the current data
        List<Float> sensorDataTransform = Utils.fourierTransform(sensorDataMagnitudeList);

        //Meaning of each index: n-th bin = n * sample frequency / NUMBER OF BINS
        for (int i = 0; i < sensorDataTransform.size() / 2; i++)
            System.out.println("The magnitude of frequency " + ((float) i) * ACCELEROMETER_SAMPLES_PER_SECOND / sensorDataTransform.size() +
                                " Hz is " + sensorDataTransform.get(i));
        //there isn't really a point in doing the fft if we are just sampling at 50hz, because we can only measure until 25hz without ambiguity.
        */




        List<List<FloatTriplet>> sensorDataListFromDatabase;

        List<SubjectActivity> activitiesToIdentify = new ArrayList<>();
        activitiesToIdentify.add(SubjectActivity.STANDING);
        activitiesToIdentify.add(SubjectActivity.WALKING);
        activitiesToIdentify.add(SubjectActivity.RUNNING);
        activitiesToIdentify.add(SubjectActivity.STAIRS);
        activitiesToIdentify.add(SubjectActivity.ELEVATOR_UP);
        activitiesToIdentify.add(SubjectActivity.ELEVATOR_DOWN);



        for(SubjectActivity activityToIdentify: activitiesToIdentify) {
            //get the recordings for each activity (if there are any, else continue)
            sensorDataListFromDatabase = dbconnection.getActivityRecordings(activityToIdentify);
            if (sensorDataListFromDatabase == null) continue;


            for(List<FloatTriplet> recording : sensorDataListFromDatabase) {


                //Separate each recording into its components
                List<Float> recordedDataX = new ArrayList<>();
                List<Float> recordedDataY = new ArrayList<>();
                List<Float> recordedDataZ = new ArrayList<>();

                for (FloatTriplet f : recording){
                    recordedDataX.add(f.getX());
                    recordedDataY.add(f.getY());
                    recordedDataZ.add(f.getZ());
                }


                //Correlate them with sensor data
                List<Float> correlationsForEachDelayX = Utils.correlation(sensorDataX, recordedDataX, minDelay, maxDelay);
                List<Float> correlationsForEachDelayY = Utils.correlation(sensorDataY, recordedDataY, minDelay, maxDelay);
                List<Float> correlationsForEachDelayZ = Utils.correlation(sensorDataZ, recordedDataZ, minDelay, maxDelay);

                //Get overall correlation
                List<Float> correlationsForEachDelayTotal = new ArrayList<>();
                for(int i = 0; i < correlationsForEachDelayX.size(); i++){
                    correlationsForEachDelayTotal.add((Math.abs(correlationsForEachDelayX.get(i)) + Math.abs(correlationsForEachDelayY.get(i)) + Math.abs(correlationsForEachDelayZ.get(i)))/3);
                }

                //Find the where there is the best correlation
                int largestCorrelationIndex = Utils.argMax(correlationsForEachDelayTotal);
                float correlationMax = correlationsForEachDelayTotal.get(largestCorrelationIndex);
                correlationMax /= sensorDataListFromDatabase.size();

                if(maxCorrelationPerActivity.containsKey(activityToIdentify)){
                    float currentMaxCorr = maxCorrelationPerActivity.get(activityToIdentify);
                    maxCorrelationPerActivity.put(activityToIdentify, currentMaxCorr + correlationMax);
                }else{
                    maxCorrelationPerActivity.put(activityToIdentify, correlationMax);
                }

            }

        }

        //NOTE: the following can be done at the same time as the correlations, so that only one iteration is needed

        //In case none of the correlations worked, try some FourierTransforms

        //List<Float> sensorDataTransform = Utils.fourierTransform(sensorDataMagnitudeList);

        //Meaning of each index: n-th bin = n * sample frequency / NUMBER OF BINS


        //note that the 0 bin will have the maximum always.

        // we can correlate each transform... Discuss features.



        SubjectActivity maxCorrelated = SubjectActivity.STANDING;
        float maxCorrelationUntilNow = 0;
        for(SubjectActivity key: maxCorrelationPerActivity.keySet()){
            System.out.println(key.name() + ": " + maxCorrelationPerActivity.get(key));
            if(maxCorrelationPerActivity.get(key) > maxCorrelationUntilNow){
                maxCorrelationUntilNow = maxCorrelationPerActivity.get(key);
                maxCorrelated = key;
            }
        }


        return maxCorrelated;
    }




    @Override
    public int getSteps(Queue<Float> sensorData, Queue<FloatTriplet> sensorDataRaw, DatabaseService dbconnection, SubjectActivity currentActivityState, AtomicInteger accReadingsSinceLastUpdate) {
        if (currentActivityState == SubjectActivity.WALKING || currentActivityState == SubjectActivity.RUNNING) {
            int numSteps = accReadingsSinceLastUpdate.get() / (60 / 2);
            int remainder = accReadingsSinceLastUpdate.get() % (60 / 2);
            //System.out.println("well, comes here..ACC_READINGS_SINCE = " + accReadingsSinceLastUpdate.get() + "\tNUM STEPS = " + numSteps + "\tREMAINDER = " + remainder + "\t OPT DELAY = " + 60);

            accReadingsSinceLastUpdate.set(remainder);

            if (currentActivityState == SubjectActivity.RUNNING) {
                numSteps *= 2; // "Thomas Running invented running when he tried to walk twice" -> running = 2 * walk
            }
            return numSteps;
        }
        accReadingsSinceLastUpdate.set(0);
        return 0;
    }
}
