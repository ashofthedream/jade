package ashes.of.jade.lang;

public class LexemNode implements Node {

    private final Lexem lexem;

    public LexemNode(Lexem lexem) {
        this.lexem = lexem;
    }

    @Override
    public String asString() {
        return lexem.getContent();
    }

    @Override
    public LexemType getType() {
        return lexem.getType();
    }

    @Override
    public String toString() {
        return "_" + lexem;
    }
}
