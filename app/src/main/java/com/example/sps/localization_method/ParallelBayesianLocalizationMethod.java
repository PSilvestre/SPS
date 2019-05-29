package com.example.sps.localization_method;

import android.net.wifi.ScanResult;

import com.example.sps.data_loader.WifiScan;
import com.example.sps.database.DatabaseService;

import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ParallelBayesianLocalizationMethod implements LocalizationMethod {


    int numBSSIDSUsed;

    @Override
    public float[] computeLocation(List<ScanResult> scan, float[] priorProbabilities, DatabaseService databaseService) {

        int numCells = priorProbabilities.length;
        double[] currProb = new double[numCells];
        boolean useBSSID;

        numBSSIDSUsed = 0;
        for (int j = 0; j < scan.size(); j++) {
            useBSSID = false;
            float normalizer = 0;
            double[] probForBssid = new double[numCells];

            for (int i = 1; i <= numCells; i++) {
                ScanResult scanResult = scan.get(j);
                NormalDistribution normal = databaseService.getGaussian(i, scanResult.BSSID);
                if (normal != null) {
                    double rssi = ((double) scanResult.level);
                    double rssiProb = (normal.cumulativeProbability(rssi + 0.5) - normal.cumulativeProbability(rssi - 0.5));
                    double rssiProbTimesPrior = rssiProb * priorProbabilities[i - 1];

                    probForBssid[i - 1] = rssiProbTimesPrior;
                    normalizer += rssiProbTimesPrior;
                    if (rssiProb > 0.0)
                        useBSSID = true;
                } else {
                    probForBssid[i - 1] = 0;
                    normalizer += 0;
                }
            }


            if (normalizer > 0) {
                for (int i = 0; i < numCells; i++) {
                    probForBssid[i] /= normalizer;
                }
            }

            float sum = 0f;
            if (useBSSID && numBSSIDSUsed == 0) {
                for (int i = 0; i < numCells; i++) {
                    currProb[i] = probForBssid[i];
                }
                numBSSIDSUsed++;
            } else if (useBSSID) {
                for (int i = 0; i < numCells; i++) {

                    currProb[i] = (currProb[i] * numBSSIDSUsed + probForBssid[i]) / (numBSSIDSUsed + 1);
                }
                numBSSIDSUsed++;
            }

        }

        float[] toReturn = new float[numCells];
        for (int i = 0; i < numCells; i++)
            toReturn[i] = (float) currProb[i];

        return toReturn;


    }

    @Override
    public String getMiscInfo() {
        return "" + numBSSIDSUsed;
    }
}
