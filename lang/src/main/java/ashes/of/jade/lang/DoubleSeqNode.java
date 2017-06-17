package ashes.of.jade.lang;

import java.util.Arrays;

public class DoubleSeqNode extends Node {

    public double[] seq;
    public DoubleSeqNode(double... seq) {
        super(new Lexem(LexemType.DoubleNumber));
        this.seq = seq;
    }

    @Override
    public DoubleSeqNode toDoubleSeq() {
        return this;
    }

    @Override
    public String toString() {
        return "SEQ" + Arrays.toString(seq);
    }
}
