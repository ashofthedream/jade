package ashes.of.jade.lang.nodes;

import ashes.of.jade.lang.Location;


public class IntNode extends Node {

    private long value;

    public IntNode(Location location, long value) {
        super(NodeType.INTEGER, location);
        this.value = value;
    }

    public IntNode(long value) {
        this(Location.EMPTY, value);
    }

    @Override
    public long toInteger() {
        return value;
    }

    @Override
    public double toDouble() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(toInteger());
    }
}
