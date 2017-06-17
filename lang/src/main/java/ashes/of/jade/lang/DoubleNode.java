package ashes.of.jade.lang;

public class DoubleNode extends Node {

    public double val;

    public DoubleNode(Lexem lexem, double val) {
        super(lexem);
        this.val = val;
    }

    public DoubleNode(double val) {
        this(new Lexem(LexemType.DoubleNumber), val);
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
