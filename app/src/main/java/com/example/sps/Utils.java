package com.example.sps;

import com.example.sps.data_loader.WifiReading;
import com.example.sps.data_loader.WifiScan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {

    public static final int NUM_BUCKETS_TO_USE = 100;

    public static Map<String, Float> calculateMeans(List<WifiScan> scansOfCell) {
        Map<String, Float> toReturn = new HashMap<>();

        //BSSID -> histogram of RSS values
        Map<String, int[]> count = new HashMap<>();


        for(WifiScan scan : scansOfCell) {
            for(WifiReading reading : scan.getReadings()) {

                if(!count.containsKey(reading.getBSSID())) {
                    count.put(reading.getBSSID(), new int[NUM_BUCKETS_TO_USE]);
                }

                count.get(reading.getBSSID())[Math.abs(reading.getRSS())]++;

            }
        }

        for(String bssid : count.keySet()){
            int[] hist = count.get(bssid);
            float mean = 0;
            int total = 0;

            for(int i = 0; i < NUM_BUCKETS_TO_USE; i++){
                total += hist[i];
            }
            for(int i = 0; i < NUM_BUCKETS_TO_USE; i++){
                mean += hist[i] * i;
            }
            toReturn.put(bssid, - mean/total);
        }
        return toReturn;
    }

    public static Map<String, Float> calculateStdDevs(List<WifiScan> scansOfCell, Map<String, Float> means) {
        Map<String, Float> toReturn = new HashMap<>();

        //BSSID -> histogram of RSS values
        Map<String, int[]> count = new HashMap<>();

        for(WifiScan scan : scansOfCell) {

            for(WifiReading reading : scan.getReadings()) {

                if(!count.containsKey(reading.getBSSID())) {
                    count.put(reading.getBSSID(), new int[NUM_BUCKETS_TO_USE]);
                }

                count.get(reading.getBSSID())[Math.abs(reading.getRSS())]++;

            }
        }

        for(String bssid : count.keySet()){
            int[] hist = count.get(bssid);
            float stddev = 0;
            float mean = means.get(bssid);
            int c = 0;
            for(int i = 0; i < NUM_BUCKETS_TO_USE; i++){
                for(int j = 0; j < hist[i]; j++) {
                    stddev += Math.pow(-i - mean, 2);
                }
                c += hist[i];
            }
            stddev /= c;


            System.out.println("std dev: " + (float) Math.sqrt(stddev));
            toReturn.put(bssid, (float) Math.sqrt(stddev));
        }

        return toReturn;
    }

}
