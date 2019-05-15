package com.example.sps.localization_method;

import android.net.wifi.ScanResult;

import com.example.sps.data_loader.WifiScan;

import java.util.List;

public class BayesianLocalizationMethod implements LocalizationMethod {


    @Override
    public float[] computeLocation(List<ScanResult> scan, float[] priorProbabilities, List<List<WifiScan>> data) {
        float[] res = {1.0f, 0.0f, 0.0f, 0.0f};
        return res;
    }

    @Override
    public String getMiscInfo() {
        return null;
    }
}
