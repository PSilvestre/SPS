package com.example.sps.localization_method;

import android.content.Context;

import com.example.sps.database.DatabaseService;

public enum LocalizationAlgorithm {


    KNN_FINGERPRINT(new FingerprintKnnLocalizationMethod()),
    KNN_RSSI(new RSSIFingerprintKnnLocalizationMethod()),
    BAYESIAN(new BayesianLocalizationMethod());

    private LocalizationMethod method;

    LocalizationAlgorithm(LocalizationMethod method) {
        this.method = method;
    } //enum constructors are automatically called

    public LocalizationMethod getMethod() {
        return method;
    }
}
