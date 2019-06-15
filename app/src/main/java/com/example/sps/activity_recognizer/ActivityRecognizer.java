package com.example.sps.activity_recognizer;

import android.app.Activity;

import com.example.sps.database.DatabaseService;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public interface ActivityRecognizer {

    SubjectActivity recognizeActivity(Queue<FloatTriplet> sensorData, DatabaseService dBconnection);

    int getSteps(Queue<FloatTriplet> sensorData, DatabaseService dBconnection, SubjectActivity currentActivityState, AtomicInteger accReadingsSinceLastUpdate);

}
