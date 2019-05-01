package com.example.sps.localization_method;

import android.net.wifi.ScanResult;

import com.example.sps.data_loader.WifiDataLoader;
import com.example.sps.data_loader.WifiReading;
import com.example.sps.data_loader.WifiSample;

import java.io.IOException;
import java.util.LinkedList;
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

        int differences = 0;

        List<String> scannedBSSID = new LinkedList<>();
        List<String> trainedBSSID = new LinkedList<>();

        for(ScanResult result : scan) {
            scannedBSSID.add(result.BSSID);
        }

        for(WifiReading reading : sample.getReadings()) {
            trainedBSSID.add(reading.getBSSID());
        }


        for(String a : scannedBSSID)
            if (! trainedBSSID.contains(a) )
                differences ++;

        for(String b : trainedBSSID)
            if (! scannedBSSID.contains(b))
                differences ++;


        return differences;
    }
}
