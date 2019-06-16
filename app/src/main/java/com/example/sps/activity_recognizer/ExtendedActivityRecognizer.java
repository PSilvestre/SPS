package com.example.sps.activity_recognizer;

import com.example.sps.Utils;
import com.example.sps.database.DatabaseService;

import java.util.ArrayList;
import java.util.List;
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

class ExtendedActivityRecognizer implements ActivityRecognizer {


    private int optDelay;

    @Override
    public SubjectActivity recognizeActivity(Queue<FloatTriplet> sensorData, DatabaseService dbconnection) {


        int minDelay = 40;
        int maxDelay = 100;

        //Get the magnitude of the acceleration sensors
        List<Float> sensorDataMagnitudeList = new ArrayList<>();
        for(FloatTriplet f : sensorData) {
            float magnitude = (float) Math.sqrt(Math.pow(f.getX(), 2) + Math.pow(f.getY(), 2) + Math.pow(f.getZ(), 2));
            sensorDataMagnitudeList.add(magnitude);
        }

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
        activitiesToIdentify.add(SubjectActivity.WALKING);
        activitiesToIdentify.add(SubjectActivity.RUNNING);
        activitiesToIdentify.add(SubjectActivity.STAIRS);
        activitiesToIdentify.add(SubjectActivity.ELEVATOR);


        List<Integer> recordingsIndexesAboveThreshold = new ArrayList<>();
        List<Float> correlationValuesAboveThreshold = new ArrayList<>();

        for(int activityIndex = 0; activityIndex < activitiesToIdentify.size(); activityIndex ++) {
            //get the recordings for each activity (if there are any, else continue)
            sensorDataListFromDatabase = dbconnection.getActivityRecordings(activitiesToIdentify.get(activityIndex));
            if (sensorDataListFromDatabase == null) continue;


            for(int recordingIndex = 0; recordingIndex < sensorDataListFromDatabase.size(); recordingIndex++) {

                //For each recording, get it to magnitudes
                List<Float> dbDataMagnitudeList = new ArrayList<>();
                for (FloatTriplet f : sensorDataListFromDatabase.get(recordingIndex)) {
                    float magnitude = (float) Math.sqrt(Math.pow(f.getX(), 2) + Math.pow(f.getY(), 2) + Math.pow(f.getZ(), 2));
                    dbDataMagnitudeList.add(magnitude);
                }

                //Correlate them
                List<Float> correlationsForEachDelay = Utils.correlation(sensorDataMagnitudeList, dbDataMagnitudeList, minDelay, maxDelay);

                //Find the where there is the best correlation
                int largestCorrelationIndex = Utils.argMax(correlationsForEachDelay);
                float correlationMax = correlationsForEachDelay.get(largestCorrelationIndex);

                //If the best correlation correlates above the threshold, add it to a list because there might be recordings
                // for that activity that do the same and we need the best fit out of those.
                if (correlationMax > 0.8) {
                    correlationValuesAboveThreshold.add(correlationMax);
                    recordingsIndexesAboveThreshold.add(recordingIndex);
                }
            }

            if (recordingsIndexesAboveThreshold.size() != 0) {
                //compute what is the correlation that has the best match out of all of them.
                int max_index = Utils.argMax(correlationValuesAboveThreshold);

                // max_index will be the recording out of the selected ones that had that maximum correlation.
                // get that recording and get the magnitude of the 3 sensors
                List<Float> magnitudeOfBestMatch = new ArrayList<>();
                for (FloatTriplet f : sensorDataListFromDatabase.get(max_index)) {
                    float magnitude = (float) Math.sqrt(Math.pow(f.getX(), 2) + Math.pow(f.getY(), 2) + Math.pow(f.getZ(), 2));
                    magnitudeOfBestMatch.add(magnitude);
                }

                //find the optDelay for that recording:
                List<Float> autocorrelation = Utils.correlation(magnitudeOfBestMatch, magnitudeOfBestMatch, minDelay, maxDelay);

                //optDelay will be the min delay + indexOfMaxAutocorrelation
                int indexOfMaxAutocorrelation = Utils.argMax(autocorrelation);

                optDelay = minDelay + indexOfMaxAutocorrelation;

                return activitiesToIdentify.get(activityIndex);
            }
        }

        //NOTE: the following can be done at the same time as the correlations, so that only one iteration is needed

        //In case none of the correlations worked, try some FourierTransforms

        //List<Float> sensorDataTransform = Utils.fourierTransform(sensorDataMagnitudeList);

        //Meaning of each index: n-th bin = n * sample frequency / NUMBER OF BINS


        //note that the 0 bin will have the maximum always.

        // we can correlate each transform... Discuss features.






        return SubjectActivity.STANDING;
    }




    @Override
    public int getSteps(Queue<FloatTriplet> sensorData, DatabaseService dbconnection, SubjectActivity currentActivityState, AtomicInteger accReadingsSinceLastUpdate) {
        if (currentActivityState == SubjectActivity.WALKING || currentActivityState == SubjectActivity.RUNNING) {
            int numSteps = accReadingsSinceLastUpdate.get() / (optDelay / 2);
            int remainder = accReadingsSinceLastUpdate.get() % (optDelay / 2);
            System.out.println("well, comes here..ACC_READINGS_SINCE = " + accReadingsSinceLastUpdate.get() + "\tNUM STEPS = " + numSteps + "\tREMAINDER = " + remainder + "\t OPT DELAY = " + optDelay);

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
