package com.example.sps.localization_method;

import android.net.wifi.ScanResult;

import java.util.List;

public interface LocalizationMethod {

    float[] computeLocation(List<ScanResult> scan, float[] priorProbabilities);

    String getMiscInfo();
}
