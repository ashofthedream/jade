package ashes.of.jade.lang;

public class SeqNode extends Node {

    private final Node l;
    private final Node r;

    public SeqNode(Lexem lexem, Node l, Node r) {
        super(lexem);

        if (!l.isInteger() || !r.isInteger())
            throw new RuntimeException("only IntNode allowed as a sequence, actual: {" + l + ", " + r + "}");

        this.l = l;
        this.r = r;
    }

    @Override
    public boolean isIntegerSeq() {
        return true;
    }

    @Override
    public IntegerSeqNode toIntegerSeq() {
        long start = l.toInteger();
        long end = r.toInteger();
        long[] seq = new long[(int) (end - start)] ;

        for (long i = 0; i < end - start; i++) {
            seq[(int) i] = start + i;
        }

        return new IntegerSeqNode(seq);
    }

    @Override
    public DoubleSeqNode toDoubleSeq() {
        long start = l.toInteger();
        long end = r.toInteger();
        double[] seq = new double[(int) (start - end)] ;

        for (long i = 0; i < end - start; i++) {
            seq[(int) i] = start = i;
        }

        return new DoubleSeqNode(seq);
    }

    @Override
    public String toString() {
        return "SEQ{" + l + ", " + r + "}";
    }
}
