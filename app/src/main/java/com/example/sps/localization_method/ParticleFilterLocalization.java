package com.example.sps.localization_method;

import android.net.wifi.ScanResult;

import com.example.sps.database.DatabaseService;
import com.example.sps.map.Cell;
import com.example.sps.map.WallPositions;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.example.sps.LocateMeActivity.NUM_CELLS;

public class ParticleFilterLocalization implements ContinuousLocalization {

    public static final int NUM_PARTICLES = 1000;
    private NormalDistribution noiseDegrees = new NormalDistribution(0, 22.5);
    private NormalDistribution noiseDistance;

    @Override
    public float[] computeLocation(List<ScanResult> scan, float[] priorProbabilities, DatabaseService databaseService) {
        return new float[0];
    }

    @Override
    public String getMiscInfo() {
        return "NUM_PARTICLES = " + NUM_PARTICLES;
    }


    private void sampleParticle(float[] cellProbabilities, Particle particle) {
        WallPositions walls = new WallPositions();

        Random r = new Random(System.currentTimeMillis());

        List<Pair<Integer,Double>> itemWeights = new ArrayList<>();
        for (int i = 0; i < cellProbabilities.length; i++) {
            itemWeights.add(new Pair(i, new Double(cellProbabilities[i])));
        }

        EnumeratedDistribution cellProbability = new EnumeratedDistribution(itemWeights);

        int cell = (int) cellProbability.sample();

        Cell cellO = walls.getCells().get(cell);

        float sampleX = r.nextFloat() * (cellO.getRightWall() - cellO.getLeftWall()) + cellO.getLeftWall();
        float sampleY = r.nextFloat() * (cellO.getBottomWall() - cellO.getTopWall()) + cellO.getTopWall();
        particle.setX(sampleX);
        particle.setY(sampleY);
        particle.setCell(cell);

    }

    private void spreadList(float[] cellProbabilities, List<Particle> particles) {
        WallPositions walls = new WallPositions();

        Random r = new Random(System.currentTimeMillis());
        List<Pair<Integer,Double>> itemWeights = new ArrayList<>();
        for (int i = 0; i < cellProbabilities.length; i++) {
            System.out.println("i: " + i + ", val: " + cellProbabilities[i]);
            itemWeights.add(new Pair(i, new Double(cellProbabilities[i])));
        }

        EnumeratedDistribution cellProbability = new EnumeratedDistribution(itemWeights);
        int cell;
        for( int i = 0; i < NUM_PARTICLES; i++){
            cell = (int) cellProbability.sample();

            Cell cellO = walls.getCells().get(cell);

            float sampleX = r.nextFloat() * (cellO.getRightWall() - cellO.getLeftWall()) + cellO.getLeftWall();
            float sampleY = r.nextFloat() * (cellO.getBottomWall() - cellO.getTopWall()) + cellO.getTopWall();
            particles.get(i).setX(sampleX);
            particles.get(i).setY(sampleY);
            particles.get(i).setCell(cell);

        }
    }



    @Override
    public CopyOnWriteArrayList<Particle> spreadParticles(float[] priorBelief) {
        // If prior belief is uniform, distribute by area. Otherwise, by belief.
        float[] spreadProbabilities = priorBelief;
        boolean allEqual = true;
        for(int i = 0; i < priorBelief.length; i++)
            if(priorBelief[Math.max(0, i-1)] != priorBelief[i]) {
                allEqual = false;
                break;
            }
        if(allEqual){
            WallPositions wallPositions = new WallPositions();
            for(int i = 0; i < priorBelief.length; i++) {
                spreadProbabilities[i] = wallPositions.getCells().get(i).getAreaOfCell() / wallPositions.getTotalArea();
            }
        }

        CopyOnWriteArrayList<Particle> particles = new CopyOnWriteArrayList<>();

        for( int i = 0; i < NUM_PARTICLES; i++) {
            Particle particle = new Particle(0, 0, 1.0f/NUM_PARTICLES, 0);
            particles.add(particle);

        }

        spreadList(spreadProbabilities, particles);


        return particles;
    }

    @Override
    public void updateParticles(float azi, float distance, CopyOnWriteArrayList<Particle> particles) {
        if (distance == 0)
            return;

        noiseDistance = new NormalDistribution(0, distance); //TODO: find good STD_DEV
        float norm;
        float angle;
        float toRadians = (float) (1.0f / 180 * Math.PI);
        for(Particle p: particles){
            norm = (float) (distance + noiseDistance.sample());
            angle = (float) (azi + noiseDegrees.sample());
            p.setX((float) (p.getX() + norm * Math.cos(angle * toRadians)));
            p.setY((float) (p.getY() + norm * Math.sin(angle * toRadians)));
        }
    }

    @Override
    public void collideAndResample(CopyOnWriteArrayList<Particle> particles, WallPositions walls) {
        LinkedList<Particle> deadParticles = new LinkedList<>();


        //collide and erase
        for(Particle p: particles){
            if (walls.getDrawable().get(p.getCell()).collide(p))
                deadParticles.add(p);
        }
        particles.removeAll(deadParticles);

        int totalTimeAlive = 0;
        boolean cellFound;
        int cell_slack = 4; //check collisions in cells in the proximity (+-cell_slack)
        for(Particle p: particles){
            cellFound = false;
            for(int i = Math.max(0, p.getCell() - cell_slack); i < Math.min(walls.getDrawable().size(), p.getCell() + cell_slack + 1); i++) {
                if(walls.getDrawable().get(i).isParticleInside(p)) {
                    p.setCell(i);
                    cellFound = true;
                    p.incTimeAlive();
                    totalTimeAlive += p.getTimeAlive();
                    break;
                }
            }
            if (!cellFound) {
                deadParticles.add(p);
            }
        }
         totalTimeAlive += deadParticles.size(); //All dead particles have time alive 1

        particles.removeAll(deadParticles);



        if(particles.size() != 0) {
            List<Pair<Particle, Double>> particleWeights = new ArrayList<>();
            for(Particle p: particles){
                p.setWeight(((float) p.getTimeAlive()) / totalTimeAlive);
                particleWeights.add(new Pair<Particle, Double>(p, (double) p.getWeight()));
            }

            EnumeratedDistribution<Particle> distribution = new EnumeratedDistribution<>(particleWeights);

            Random r = new Random(System.currentTimeMillis());

            float[] spreadProbabilities = new float[NUM_CELLS];
            WallPositions wallPositions = new WallPositions();
            for(int i = 0; i <NUM_CELLS; i++) {
                spreadProbabilities[i] = wallPositions.getCells().get(i).getAreaOfCell() / wallPositions.getTotalArea();
            }

            for(Particle p: deadParticles){
                p.resetTimeAlive();
                p.setWeight(((float) p.getTimeAlive()) / totalTimeAlive);

                if(r.nextFloat() < 0.1){

                    sampleParticle(spreadProbabilities, p);
                }else {
                    Particle selected = distribution.sample();
                    p.setY(selected.getY());
                    p.setX(selected.getX());
                    p.setCell(selected.getCell());
                }
            }
            particles.addAll(deadParticles);
        } else {
            float[] belief = new float[NUM_CELLS];
            particles = spreadParticles(belief);
        }
    }

}
