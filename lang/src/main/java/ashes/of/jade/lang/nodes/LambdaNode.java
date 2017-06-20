package ashes.of.jade.lang.nodes;

import ashes.of.jade.lang.Location;

import java.util.ArrayDeque;
import java.util.Deque;



public class LambdaNode extends Node {

    public Deque<Node> stack = new ArrayDeque<>();

    public LambdaNode(Location location) {
        super(NodeType.LAMBDA, location);
    }

    @Override
    public Deque<Node> getNodes() {
        return stack;
    }

    @Override
    public String toString() {
        return "LAMBDA{" + stack + "}";
    }
}
