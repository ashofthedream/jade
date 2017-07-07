package ashes.of.jade.lang.nodes;

import ashes.of.jade.lang.Location;


public class SequenceNode extends Node {

    public Node[] seq;

    public SequenceNode(Location location, long start, long end) {
        super(NodeType.SEQUENCE, location);
        long length = (end - start) + 1;
        seq = new Node[ (int) length];
        for (int i = 0; i < length; i++)
            seq[i] = new IntNode(start + i);
    }

    @Override
    public SequenceNode toSeq() {
        return this;
    }


    public int size() {
        return seq.length;
    }

    @Override
    public String toString() {
        return "[" + seq[0] + ".." + seq[seq.length - 1] + "]";
    }
}
