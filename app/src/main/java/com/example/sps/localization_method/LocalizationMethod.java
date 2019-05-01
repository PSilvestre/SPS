package com.example.sps.localization_method;

import android.net.wifi.ScanResult;

import java.util.List;

public interface LocalizationMethod {

    int computeLocation(List<ScanResult> scan);

}
