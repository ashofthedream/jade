package ashes.of.jade.lang;

public class IntNode implements Node {

    private long val;

    public IntNode(long val) {
        this.val = val;
    }

    @Override
    public LexemType getType() {
        return LexemType.IntegerNumber;
    }

    @Override
    public boolean isInteger() {
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
        return val;
    }

    @Override
    public Node eval() {
        return this;
    }

    @Override
    public String toString() {
        return "IntNode{" + val + '}';
    }
}
