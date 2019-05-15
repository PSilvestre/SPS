package com.example.sps.localization_method;

import android.net.wifi.ScanResult;

import com.example.sps.data_loader.WifiScan;

import java.util.List;

public interface LocalizationMethod {

    float[] computeLocation(List<ScanResult> scan, float[] priorProbabilities, List<List<WifiScan>> data);

    String getMiscInfo();
}
