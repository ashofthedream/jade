package ashes.of.jade.lang;

import java.util.Arrays;

public class IntegerSeqNode extends Node {

    public long[] seq;
    public IntegerSeqNode(long... seq) {
        super(new Lexem(LexemType.IntegerSeq));
        this.seq = seq;
    }

    @Override
    public IntegerSeqNode toIntegerSeq() {
        return this;
    }

    @Override
    public String toString() {
        return "SEQ" + Arrays.toString(seq);
    }
}
