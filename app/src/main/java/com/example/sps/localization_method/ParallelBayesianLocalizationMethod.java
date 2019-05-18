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


    @Override
    public float[] computeLocation(List<ScanResult> scan, float[] priorProbabilities,  DatabaseService databaseService) {

        int numCells = priorProbabilities.length;
        double[] currProb = new double[numCells];
        for(int j = 0; j < scan.size(); j++) {

            float normalizer = 0;
            double[] probForBssid = new double[numCells];

            for(int i = 1; i <= numCells; i++) {
                ScanResult scanResult = scan.get(j);
                NormalDistribution normal = databaseService.getGaussian(i, scanResult.BSSID);
                double rssi = ((double) scanResult.level);
                double rssiProb = (normal.cumulativeProbability(rssi + 0.5) - normal.cumulativeProbability(rssi - 0.5));
                double rssiProbTimesPrior = rssiProb * priorProbabilities[i];
                probForBssid[i-1] = rssiProbTimesPrior;
                normalizer += rssiProbTimesPrior;
            }

            for (int i = 0; i < numCells; i++) {
                probForBssid[i] /= normalizer;
            }

            if(j == 0) {
                currProb = probForBssid;
            }

            if(j > 0) {
                for (int i = 0; i < numCells; i++) {
                    currProb[i] = (currProb[i] * j + probForBssid[i]) / (j + 1) ;
                }
            }

        }

        float[] toReturn = new float[numCells];
        for(int i = 0; i < numCells; i++)
            toReturn[i] = (float) currProb[i];

        return toReturn;


    }

    @Override
    public String getMiscInfo() {
        return null;
    }
}
