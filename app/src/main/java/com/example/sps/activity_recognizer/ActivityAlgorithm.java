package com.example.sps.activity_recognizer;


public enum ActivityAlgorithm {

    NORMAL_STD(new StdDevActivityRecognizer()),
    NORMAL_AUTOCORR(new AutocorrActivityRecognizer()),
    EXTENDED_CROSSCORR(new ExtendedActivityRecognizer());



    private ActivityRecognizer method;

    ActivityAlgorithm(ActivityRecognizer method) {
        this.method = method;
    } //enum constructors are automatically called

    public ActivityRecognizer getMethod() {
        return method;
    }
}
