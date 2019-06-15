package com.example.sps.activity_recognizer;

import com.example.sps.database.DatabaseService;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;


/* This activity is the same as AutocorrActivityRecognizer but it will correlate with many more
activities apart from just Walking.
RUNNING
STAIRS
ELEVATOR

It should return the one that autocorrelates the best or that correlates to a min threshold,
whatever happens first.
 */

class ExtendedActivityRecognizer implements ActivityRecognizer {


    @Override
    public SubjectActivity recognizeActivity(Queue<FloatTriplet> sensorData, DatabaseService dbconnection) {
        return null;
    }

    @Override
    public int getSteps(Queue<FloatTriplet> sensorData, DatabaseService dBconnection, SubjectActivity currentActivityState, AtomicInteger accReadingsSinceLastUpdate) {
        return 0;
    }
}
