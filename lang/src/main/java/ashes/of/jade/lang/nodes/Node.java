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

    public boolean is(NodeType type) {
        return type == getType();
    }

    public boolean is(NodeType... types) {
        for (NodeType type : types) {
            if (!is(type))
                return false;
        }

        return true;
    }

    public boolean isString() {
        return is(NodeType.STRING);
    }

    public boolean isDoubleSeq() {
        return is(NodeType.DOUBLESEQ);
    }

    public boolean isDouble() {
        return is(NodeType.DOUBLE);
    }

    public boolean isIntegerSeq() {
        return is(NodeType.INTEGERSEQ);
    }

    public boolean isInteger() {
        return is(NodeType.INTEGER);
    }

    public Deque<Node> getNodes() {
        return null;
    }

    public long toInteger() {
        return 0;
    }

    public IntegerSeqNode toIntegerSeq() {
        return null;
    }

    public double toDouble() {
        return 0;
    }

    public DoubleSeqNode toDoubleSeq() {
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

    public boolean isNumber() {
        return isInteger() || isDouble();
    }
}