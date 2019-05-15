package com.example.sps.data_loader;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.example.sps.LocateMeActivity.NUM_CELLS;

public class WifiDataLoader {

    List<List<WifiScan>> readingsPerCell;

    public WifiDataLoader() {
        readingsPerCell = new ArrayList<>(NUM_CELLS);
    }

    public List<List<WifiScan>> load() throws IOException {
        for(int i = 0; i < NUM_CELLS; i++){

            List<WifiScan> samplesOfThisCell = new LinkedList<>();

            WifiScan sample = new WifiScan();
            List<WifiReading> readings = sample.getReadings();

            BufferedReader reader = new BufferedReader(new FileReader(Environment.getExternalStorageDirectory() + "/sps/readings" + (i + 1) + ".txt"));
            String line = reader.readLine();

            while(line != null){
                if(line.equals("")){
                    samplesOfThisCell.add(sample);
                    sample = new WifiScan();
                    readings = sample.getReadings();
                }else {
                    readings.add(WifiReading.fromString(line));
                }


                line = reader.readLine();
            }

            readingsPerCell.add(samplesOfThisCell);
        }
    return readingsPerCell;
    }
}
