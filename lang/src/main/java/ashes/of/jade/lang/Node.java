package ashes.of.jade.lang;


import java.util.Deque;

public class Node {

    protected final Lexem lexem;

    public Node(Lexem lexem) {
        this.lexem = lexem;
    }

    public Lexem getLexem() {
        return lexem;
    }

    public Location getLocation() {
        return lexem.getLocation();
    }

    public LexemType getType() {
        return lexem.getType();
    }

    public boolean is(LexemType type) {
        return type == getType();
    }

    public boolean isLambda() {
        return is(LexemType.LAMBDA);
    }

    public boolean isString() {
        return is(LexemType.String);
    }

    public boolean isDoubleSeq() {
        return is(LexemType.DoubleSeq);
    }

    public boolean isDouble() {
        return is(LexemType.DoubleNumber);
    }

    public boolean isIntegerSeq() {
        return is(LexemType.IntegerSeq);
    }

    public boolean isInteger() {
        return is(LexemType.IntegerNumber);
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
        return lexem.getContent();
    }

    @Override
    public String toString() {
        return lexem.toString();
    }
}