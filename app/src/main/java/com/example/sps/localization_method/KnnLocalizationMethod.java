package com.example.sps.localization_method;

import android.net.wifi.ScanResult;

import com.example.sps.data_loader.WifiDataLoader;
import com.example.sps.data_loader.WifiSample;

import java.io.IOException;
import java.util.List;

public class KnnLocalizationMethod implements LocalizationMethod {


    private List<List<WifiSample>> data;
    public KnnLocalizationMethod(){
        WifiDataLoader loader = new WifiDataLoader();
        try {
            data = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public int computeLocation(List<ScanResult> scan) {
        int cell = 0;





        return cell;
    }

    public int calculateDistance(List<ScanResult> scan, WifiSample sample){
        return 0;
    }
}
