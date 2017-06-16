package ashes.of.jade.lang;

public class DoubleNode implements Node {

    private double val;

    public DoubleNode(double val) {
        this.val = val;
    }

    @Override
    public LexemType getType() {
        return LexemType.DoubleNumber;
    }

    @Override
    public boolean isDouble() {
        return true;
    }

    @Override
    public String asString() {
        return String.valueOf(val);
    }

    @Override
    public double asDouble() {
        return val;
    }

    @Override
    public long asInteger() {
        return Math.round(val);
    }

    @Override
    public Node eval() {
        return this;
    }


    @Override
    public String toString() {
        return "DoubleNode{" + val + '}';
    }
}
