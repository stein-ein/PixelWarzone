package com.github.steinein.pixelwarzone.selection;

public class Point {

    private final int x;
    private final int z;

    public Point(final int x, final int z) {
        this.x = x;
        this.z = z;
    }

    public int getX() {
        return this.x;
    }

    public int getZ() {
        return this.z;
    }

    @Override
    public String toString() {
        return "(x: " + this.x + ", z: " + this.z + ")";
    }
}
