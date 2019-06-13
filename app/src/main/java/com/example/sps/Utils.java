package com.example.sps;

import com.example.sps.activity_recognizer.FloatTriplet;
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


    public static FloatTriplet Mean(List<FloatTriplet> list) {

        FloatTriplet mean = new FloatTriplet(0, 0, 0);
        for (int i = 0; i < list.size(); i++) {
            mean.setX(mean.getX() + list.get(i).getX());
            mean.setY(mean.getY() + list.get(i).getY());
            mean.setZ(mean.getZ() + list.get(i).getZ());
        }
        mean.setX(mean.getX()/list.size());
        mean.setY(mean.getY()/list.size());
        mean.setZ(mean.getZ()/list.size());

        return mean;
    }

    public static FloatTriplet StdDeviation(List<FloatTriplet> list, FloatTriplet mean) {

        FloatTriplet stdDev = new FloatTriplet(0, 0, 0);
        for (int i = 0; i < list.size(); i++) {
            stdDev.setX(stdDev.getX() + (float) Math.pow(list.get(i).getX() - mean.getX(),2));
            stdDev.setY(stdDev.getY() + (float) Math.pow(list.get(i).getY() - mean.getY(),2));
            stdDev.setZ(stdDev.getZ() + (float) Math.pow(list.get(i).getZ() - mean.getZ(),2));
        }
        stdDev.setX((float)Math.sqrt(stdDev.getX()/list.size()));
        stdDev.setY((float)Math.sqrt(stdDev.getY()/list.size()));
        stdDev.setZ((float)Math.sqrt(stdDev.getZ()/list.size()));

        return stdDev;
    }

}
