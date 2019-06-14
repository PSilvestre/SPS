package com.example.sps;

import com.example.sps.activity_recognizer.FloatTriplet;
import com.example.sps.data_loader.WifiReading;
import com.example.sps.data_loader.WifiScan;

import java.util.ArrayList;
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


            toReturn.put(bssid, (float) Math.sqrt(stddev));
        }

        return toReturn;
    }


    public static float mean(List<Float> list) {

        float mean = 0;
        for (int i = 0; i < list.size(); i++) {
            mean += list.get(i);
        }
        mean /= list.size();

        return mean;
    }

    public static float stdDeviation(List<Float> list, Float mean) {

        float stdDev = 0;
        for (int i = 0; i < list.size(); i++) {
            stdDev += (float) Math.pow(list.get(i) - mean,2);
        }
        stdDev /= list.size();
        stdDev = (float) Math.sqrt(stdDev);

        return stdDev;
    }


    public static List<Float> correlation(List<Float> data1, List<Float> data2, int minDelay, int maxDelay) {


        List<Float> array1;
        List<Float> array2;

        Float mean1, mean2;
        Float stdDev1, stdDev2;


        List<Float> correlationForEachDelay = new ArrayList<>();

        for (int delay = minDelay; delay < maxDelay; delay++) {
            float sum = 0;

            array1 = data1.subList(0, delay);
            array2 = data2.subList(delay, 2 * delay);
            mean1 = Utils.mean(array1);
            mean2 = Utils.mean(array2);
            stdDev1 = Utils.stdDeviation(array1, mean1);
            stdDev2 = Utils.stdDeviation(array2, mean2);

            for (int k = 0; k < delay-1; k++) {
                sum += (array1.get(k) - mean1) * (array2.get(k) - mean2);
            }
            sum /= (delay * stdDev1 * stdDev2);
            correlationForEachDelay.add(sum);
        }

        return correlationForEachDelay;
    }

}
