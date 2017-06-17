package ashes.of.jade.lang;

public class SeqNode extends Node {

    private final Node l;
    private final Node r;

    public SeqNode(Lexem lexem, Node l, Node r) {
        super(lexem);
        this.l = l;
        this.r = r;
    }

    @Override
    public String toString() {
        return "SEQ{" + l + ", " + r + "}";
    }
}
