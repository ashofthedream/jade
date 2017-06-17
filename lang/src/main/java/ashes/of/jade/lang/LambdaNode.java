package ashes.of.jade.lang;

import java.util.ArrayDeque;
import java.util.Deque;


public class LambdaNode extends Node {

    public Deque<Node> stack = new ArrayDeque<>();

    public LambdaNode(Lexem lexem) {
        super(lexem);
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
