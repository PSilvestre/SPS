package com.example.sps.data_loader;

import java.util.LinkedList;
import java.util.List;

public class WifiSample {

    private List<WifiReading> sample;

    public WifiSample() {
        sample = new LinkedList<>();
    }

    public WifiSample(List<WifiReading> sample) {
        this.sample = sample;
    }

    public List<WifiReading> getSample() {
        return sample;
    }

    public void setSample(List<WifiReading> sample) {
        this.sample = sample;
    }
}
