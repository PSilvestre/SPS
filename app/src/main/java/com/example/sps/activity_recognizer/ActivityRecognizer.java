package com.example.sps.activity_recognizer;

import android.app.Activity;

import java.util.List;
import java.util.Queue;

public interface ActivityRecognizer {

    SubjectActivity recognizeActivity(Queue<FloatTriplet> sensorData);

}
