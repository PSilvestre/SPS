package com.example.sps.map;

public class Cell {

    public static final int TOP = 0;
    public static final int RIGHT = 1;
    public static final int BOTTOM = 2;
    public static final int LEFT= 3;

    float[] walls;
    boolean[] traversable;
    int cellNumber;

    public Cell(int cellNumber, float[] walls, boolean[] traversable) {
        this.walls = walls;
        this.cellNumber = cellNumber;
        this.traversable = traversable; //TODO: the walls should be transversable, not the cells..
    }

    public float[] getWalls() {
        return walls;
    }

    public void setWalls(float[] walls) {
        this.walls = walls;
    }

    public int getCellNumber() {
        return cellNumber;
    }

    public void setCellNumber(int cellNumber) {
        this.cellNumber = cellNumber;
    }

    public boolean[] getTraversable() {
        return traversable;
    }

    public void setTraversable(boolean[] traversable) {
        this.traversable = traversable;
    }

    public float getTopWall() {
        return walls[TOP];
    }


    public float getRightWall() {
        return walls[RIGHT];
    }


    public float getBottomWall() {
        return walls[BOTTOM];
    }


    public float getLefttWall() {
        return walls[LEFT];
    }
}
