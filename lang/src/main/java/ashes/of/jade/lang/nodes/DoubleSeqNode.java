package ashes.of.jade.lang.nodes;

import java.util.Arrays;


public class DoubleSeqNode extends Node {

    public double[] seq;
    public DoubleSeqNode(double... seq) {
        super(NodeType.DOUBLESEQ);
        this.seq = seq;
    }

    @Override
    public DoubleSeqNode toDoubleSeq() {
        return this;
    }

    @Override
    public String toString() {
        return Arrays.toString(seq);
    }
}
