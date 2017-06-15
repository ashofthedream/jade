package ashes.of.jade.lang;

public class SeqNode implements Node {

    @Override
    public LexemType getType() {
        return LexemType.Seq;
    }

    @Override
    public boolean isSeq() {
        return true;
    }

    @Override
    public Node eval() {
        return null;
    }
}
