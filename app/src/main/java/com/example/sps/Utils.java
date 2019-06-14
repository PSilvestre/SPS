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


    public static FloatTriplet mean(List<FloatTriplet> list) {

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

    public static FloatTriplet stdDeviation(List<FloatTriplet> list, FloatTriplet mean) {

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


    public static List<FloatTriplet> correlation(List<FloatTriplet> data1, List<FloatTriplet> data2, int minDelay, int maxDelay) {


        List<FloatTriplet> array1;
        List<FloatTriplet> array2;

        FloatTriplet mean1, mean2;
        FloatTriplet stdDev1, stdDev2;


        List<FloatTriplet> correlationForEachDelay = new ArrayList<>();

        for (int delay = minDelay; delay < maxDelay; delay++) {
            FloatTriplet sum = new FloatTriplet(0, 0, 0);

            array1 = data1.subList(0, delay - 1);
            array2 = data2.subList(delay, 2 * delay - 1);
            mean1 = Utils.mean(array1);
            mean2 = Utils.mean(array2);
            stdDev1 = Utils.stdDeviation(array1, mean1);
            stdDev2 = Utils.stdDeviation(array2, mean2);

            for (int k = 0; k < delay-1; k++) {
                sum.setX(sum.getX() + ((array1.get(k).getX() - mean1.getX()) * (array2.get(k).getX() - mean2.getX())));
                sum.setY(sum.getY() + ((array1.get(k).getY() - mean1.getY()) * (array2.get(k).getY() - mean2.getY())));
                sum.setZ(sum.getZ() + ((array1.get(k).getZ() - mean1.getZ()) * (array2.get(k).getZ() - mean2.getZ())));
            }
            sum.setX(sum.getX() / (delay * stdDev1.getX() * stdDev2.getX()));
            sum.setY(sum.getY() / (delay * stdDev1.getY() * stdDev2.getY()));
            sum.setZ(sum.getZ() / (delay * stdDev1.getZ() * stdDev2.getZ()));
            correlationForEachDelay.add(sum);
        }

        return correlationForEachDelay;
    }

}
