package ashes.of.jade.lang.nodes;

import ashes.of.jade.lang.Location;

import java.util.Deque;

public class Node {

    protected final NodeType type;
    protected final Location location;
    protected final String content;

    public Node(NodeType type, Location location, String content) {
        this.type = type;
        this.location = location;
        this.content = content;
    }

    public Node(NodeType type, Location location) {
        this(type, location, "");
    }

    public Node(NodeType type) {
        this(type, Location.EMPTY);
    }

    public Location getLocation() {
        return location;
    }

    public NodeType getType() {
        return type;
    }

    public Deque<Node> getNodes() {
        return null;
    }

    public boolean is(NodeType type) {
        return type == getType();
    }

    public boolean isString() {
        return is(NodeType.STRING);
    }


    public boolean isDouble() {
        return is(NodeType.DOUBLE);
    }

    public double toDouble() {
        return 0;
    }

    public boolean isInteger() {
        return is(NodeType.INTEGER);
    }

    public long toInteger() {
        return 0;
    }

    public boolean isNumber() {
        return isInteger() || isDouble();
    }


    public boolean isSeq() {
        return is(NodeType.SEQUENCE);
    }

    public SequenceNode toSeq() {
        return null;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return type + (content != null && !content.isEmpty() ? "{" + content + "}" : "" ) +
                      (location == Location.EMPTY ? "" : " " + location);
    }
}