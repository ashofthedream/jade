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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DoubleNode that = (DoubleNode) o;

        return Double.compare(that.value, value) == 0;
    }

    @Override
    public int hashCode() {
        long temp = Double.doubleToLongBits(value);
        return (int) (temp ^ (temp >>> 32));
    }

    @Override
    public String toString() {
        return String.valueOf(toDouble());
    }
}
