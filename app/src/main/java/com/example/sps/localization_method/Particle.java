package com.example.sps.localization_method;

public class Particle {
    private float x, y;
    private float weight;
    private int cell;


    public Particle(float x, float y, float weight, int cell) {
        this.x = x;
        this.y = y;
        this.weight = weight;
        this.cell = cell;
    }

    public int getCell() {
        return cell;
    }

    public void setCell(int cell) {
        this.cell = cell;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }
}
