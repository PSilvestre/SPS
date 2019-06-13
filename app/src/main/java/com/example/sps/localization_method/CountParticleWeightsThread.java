package com.example.sps.localization_method;

import android.app.Activity;

import com.example.sps.LocateMeActivity;
import com.example.sps.map.WallPositions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CountParticleWeightsThread extends Thread {


    private List<Particle> particles;
    private WallPositions walls;
    private LocateMeActivity src;

    public CountParticleWeightsThread(List<Particle> particles, WallPositions walls, LocateMeActivity src) {
        this.particles = particles;
        this.walls = walls;
        this.src = src;
    }

    @Override
    public void run() {

        while (src.getLocalizationMethod() instanceof ContinuousLocalization) {
            List<Float> weightSumPerCell = new ArrayList<>(16);

            for (int i = 0; i < walls.getCells().size(); i++) {
                weightSumPerCell.add(0.0f);
            }

            int index;

            for (Particle p : particles) {
                index = p.getCell() - 1;
                weightSumPerCell.set(index, weightSumPerCell.get(index) + p.getWeight());
            }


            int indexOfHighest = 0;
            float valueOfMax = weightSumPerCell.get(0);
            for (int i = 0; i < weightSumPerCell.size(); i++) {
                if (valueOfMax < weightSumPerCell.get(i)) {
                    indexOfHighest = i;
                    valueOfMax = weightSumPerCell.get(i);
                }
            }

            src.setLocationTextForParticleFilter(indexOfHighest + 1);

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
