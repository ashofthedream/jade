package ashes.of.jade.lang;

public class IntNode extends Node {

    public long val;

    public IntNode(Lexem lexem, long val) {
        super(lexem);
        this.val = val;
    }

    public IntNode(long val) {
        this(new Lexem(LexemType.IntegerNumber), val);
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
