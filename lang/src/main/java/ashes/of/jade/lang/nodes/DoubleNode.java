package ashes.of.jade.lang.nodes;

import ashes.of.jade.lang.Location;


public class DoubleNode extends Node {

    private double value;

    public DoubleNode(Location location, double value) {
        super(NodeType.DOUBLE, location);
        this.value = value;
    }

    public DoubleNode(double value) {
        this(Location.EMPTY, value);
    }

    @Override
    public double toDouble() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(toDouble());
    }
}
