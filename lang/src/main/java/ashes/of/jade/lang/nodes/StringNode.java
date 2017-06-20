package ashes.of.jade.lang.nodes;

import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.lexer.Lexem;
import ashes.of.jade.lang.nodes.Node;

public class StringNode extends Node {

    private String value;

    public StringNode(Location location, String value) {
        super(NodeType.STRING, location);
        this.value = value;
    }

    public StringNode(String value) {
        this(Location.EMPTY, value);
    }

    @Override
    public String toString() {
        return value;
    }
}
