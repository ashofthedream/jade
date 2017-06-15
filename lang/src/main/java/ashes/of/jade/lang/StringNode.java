package ashes.of.jade.lang;

public class StringNode implements Node {

    private String value;

    public StringNode(String value) {
        this.value = value;
    }

    @Override
    public boolean isString() {
        return true;
    }
}
