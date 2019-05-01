package com.example.sps.localization_method;

import android.net.wifi.ScanResult;

import com.example.sps.data_loader.WifiDataLoader;
import com.example.sps.data_loader.WifiReading;
import com.example.sps.data_loader.WifiSample;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static com.example.sps.LocateMeActivity.NUM_CELLS;

public class KnnLocalizationMethod implements LocalizationMethod {
    private static final int NUM_NEIGHBOURS = 10;
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

        List<Distance> distances = new LinkedList<>();
        for(int i = 0; i < data.size(); i++){

            for(WifiSample sample : data.get(i))
                distances.add(new Distance(i,calculateDistance(scan, sample)));

        }

        Collections.sort(distances, new Comparator<Distance>() {
            @Override
            public int compare(Distance distance, Distance t1) {
                return distance.getDistance() - t1.getDistance();
            }
        });

        List<Distance> closest = distances.subList(0, NUM_NEIGHBOURS);

        int cellWinner = countVotes(closest);




        return cell;
    }

    private int countVotes(List<Distance> closest) {
        int[] votes = new int[NUM_CELLS];

        for(Distance d : closest)
            votes[d.getCell()]++;

        int highest_i = -1;
        int highest_val = -1;
        for(int i = 0; i < votes.length; i++){
            if(votes[i] > highest_val){
                highest_i = i;
                highest_val = votes[i];
            }
        }
        return highest_i;
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

    public class Distance {
        private int cell;
        private int distance;

        public Distance(int cell, int distance) {
            this.cell = cell;
            this.distance = distance;
        }

        public int getCell() {
            return cell;
        }

        public void setCell(int cell) {
            this.cell = cell;
        }

        public int getDistance() {
            return distance;
        }

        public void setDistance(int distance) {
            this.distance = distance;
        }
    }
}
