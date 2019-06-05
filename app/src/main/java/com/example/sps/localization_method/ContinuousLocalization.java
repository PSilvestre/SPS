package com.example.sps.localization_method;

import android.net.wifi.ScanResult;

import com.example.sps.database.DatabaseService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public interface ContinuousLocalization extends LocalizationMethod {

    CopyOnWriteArrayList<Particle> spreadParticles(float[] priorBelief);

    void updateParticles(float azi, float distance, CopyOnWriteArrayList<Particle> particles);
}
