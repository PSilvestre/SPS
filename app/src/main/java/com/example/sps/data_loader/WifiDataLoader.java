package com.example.sps.data_loader;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.example.sps.LocateMeActivity.NUM_CELLS;

public class WifiDataLoader {

    List<List<WifiSample>> readingsPerCell;

    public WifiDataLoader() {
        readingsPerCell = new ArrayList<>(NUM_CELLS);
    }

    public List<List<WifiSample>> load() throws IOException {
        for(int i = 0; i < NUM_CELLS; i++){

            List<WifiSample> samplesOfThisCell = new LinkedList<>();

            WifiSample sample = new WifiSample();
            List<WifiReading> readings = sample.getSample();

            BufferedReader reader = new BufferedReader(new FileReader(Environment.getExternalStorageDirectory() + "/sps/readings" + i));
            String line = reader.readLine();

            while(line != null){
                if(line.equals("")){
                    samplesOfThisCell.add(sample);
                    sample = new WifiSample();
                    readings = sample.getSample();
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
