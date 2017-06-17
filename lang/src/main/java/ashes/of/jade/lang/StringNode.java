package ashes.of.jade.lang;

public class StringNode extends Node {

    private String value;

    public StringNode(Lexem lexem, String value) {
        super(lexem);
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
