package ashes.of.jade.lang;

public class IntNode extends Node {

    private long val;

    public IntNode(Lexem lexem, long val) {
        super(lexem);
        this.val = val;
    }

    @Override
    public long toInteger() {
        return val;
    }

    @Override
    public String toString() {
        return String.valueOf(toInteger());
    }
}
