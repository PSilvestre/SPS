package com.example.sps.activity_recognizer;

import android.app.Activity;

import java.util.List;

public interface ActivityRecognizer {

    SubjectActivity recognizeActivity(List<FloatTriplet> sensorData);

}
