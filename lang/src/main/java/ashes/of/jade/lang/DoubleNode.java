package ashes.of.jade.lang;

public class DoubleNode extends Node {

    private double val;

    public DoubleNode(Lexem lexem, double val) {
        super(lexem);
        this.val = val;
    }

    @Override
    public double toDouble() {
        return val;
    }

    @Override
    public String toString() {
        return String.valueOf(toDouble());
    }
}
