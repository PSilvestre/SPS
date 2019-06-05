package com.example.sps.localization_method;

import android.net.wifi.ScanResult;

import com.example.sps.database.DatabaseService;
import com.example.sps.map.Cell;
import com.example.sps.map.WallPositions;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class ParticleFilterLocalization implements ContinuousLocalization {

    public static final int NUM_PARTICLES = 1000;

    @Override
    public float[] computeLocation(List<ScanResult> scan, float[] priorProbabilities, DatabaseService databaseService) {
        return new float[0];
    }

    @Override
    public String getMiscInfo() {
        return null;
    }

    @Override
    public CopyOnWriteArrayList<Particle> spreadParticles(float[] priorBelief) {
        CopyOnWriteArrayList<Particle> particles = new CopyOnWriteArrayList<>();


        WallPositions walls = new WallPositions();

        Random r = new Random(System.currentTimeMillis());
        List<Pair<Integer,Double>> itemWeights = new ArrayList<>();
        for (int i = 0; i < priorBelief.length; i++) {
            itemWeights.add(new Pair(i, new Double(priorBelief[i])));
        }

        EnumeratedDistribution cellProbability = new EnumeratedDistribution(itemWeights);
        int cell;
        for( int i = 0; i < NUM_PARTICLES; i++){
            cell = (int) cellProbability.sample();

            Cell cellO = walls.getCells().get(cell);

            float sampleX = r.nextFloat() * (cellO.getRightWall() - cellO.getLefttWall()) + cellO.getLefttWall();
            float sampleY = r.nextFloat() * (cellO.getBottomWall() - cellO.getTopWall()) + cellO.getTopWall();

            Particle particle = new Particle(sampleX, sampleY, 1/NUM_PARTICLES, cell);

            particles.add(particle);
        }
        return particles;
    }
}
