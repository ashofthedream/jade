package ashes.of.jade.lang.nodes;

import java.util.Arrays;


public class IntegerSeqNode extends Node {

    public long[] seq;

    public IntegerSeqNode(long... seq) {
        super(NodeType.INTEGERSEQ);
        this.seq = seq;
    }

    @Override
    public IntegerSeqNode toIntegerSeq() {
        return this;
    }

    @Override
    public String toString() {
        return Arrays.toString(seq);
    }
}
