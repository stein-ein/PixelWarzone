package com.github.steinein.pixelwarzone.selection;

public class WarzoneSelection {

    private Point firstPos = null;
    private Point secondPos = null;
    private String world;

    public WarzoneSelection() {}

    public WarzoneSelection(final Point firstPos, final Point secondPos, final String world) {
        this.firstPos = firstPos;
        this.secondPos = secondPos;
        this.world = world;
    }

    public void setFirstPos(final Point firstPos) { this.firstPos = firstPos; }
    public void setSecondPos(final Point secondPos) { this.secondPos = secondPos; }
    public void setWorld(String world) { this.world = world; }

    public boolean isCompleteSelection() {
        return (this.firstPos != null) && (this.secondPos != null);
    }

    public Point getFirstPos() { return this.firstPos; }
    public Point getSecondPos() { return this.secondPos; }
    public String getWorld() { return this.world; }

    public int greaterX() { return Math.max(this.firstPos.getX(), this.secondPos.getX()); }
    public int lesserX() { return Math.min(this.firstPos.getX(), this.secondPos.getX()); }
    public int greaterZ() { return Math.max(this.firstPos.getZ(), this.secondPos.getZ()); }
    public int lesserZ() { return Math.min(this.firstPos.getZ(), this.secondPos.getZ()); }
}
