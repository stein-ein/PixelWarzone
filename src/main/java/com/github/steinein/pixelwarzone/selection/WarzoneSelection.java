package com.github.steinein.pixelwarzone.selection;

public class WarzoneSelection {

    /*

    We just need an axis-aligned rectangle, nothing fancy.

    In other words, given two points A(x1, y1) and B(x2, y2) that are diagonal to each other, it follows that
    C(x1, y2) and D(x2, y1) will produce a rectangle.

    Since Minecraft is 3D, we have x, y and z axes. We only care about x and z though, while y (height) is ignored.

    */

    private Point firstPos = null;
    private Point secondPos = null;

    public WarzoneSelection() {}

    public WarzoneSelection(final Point firstPos, final Point secondPos) {
        this.firstPos = firstPos;
        this.secondPos = secondPos;
    }

    public void setFirstPos(final Point firstPos) {
        this.firstPos = firstPos;
    }

    public void setSecondPos(final Point secondPos) {
        this.secondPos = secondPos;
    }

    public boolean isCompleteSelection() {
        return (this.firstPos != null) && (this.secondPos != null);
    }

    public Point getFirstPos() {
        return this.firstPos;
    }

    public Point getSecondPos() {
        return this.secondPos;
    }

    public int greaterX() {
        return Math.max(this.firstPos.getX(), this.secondPos.getX());
    }

    public int lesserX() {
        return Math.min(this.firstPos.getX(), this.secondPos.getX());
    }

    public int greaterZ() {
        return Math.max(this.firstPos.getZ(), this.secondPos.getZ());
    }

    public int lesserZ() {
        return Math.min(this.firstPos.getZ(), this.secondPos.getZ());
    }

}
