package ashes.of.jade.lang;

import ashes.of.jade.lang.nodes.Node;
import ashes.of.jade.lang.nodes.NodeType;

import java.util.Deque;

import static org.junit.Assert.assertEquals;

public class NodeAssert {


    public static void assertNode(NodeType expected, Node n) {
        assertEquals(expected, n.getType());
    }

    public static void assertNode(NodeType expected, String content, Node n) {
        assertEquals(expected, n.getType());
        assertEquals(content, n.getContent());
    }

    public static void assertNodeType(NodeType expected, Node n) {
        assertEquals(expected, n.getType());
    }


    public static void assertPlus(Deque<Node> rpn) {
        assertNodeType(NodeType.ADD, rpn.removeLast());
    }

    public static void assertMinus(Deque<Node> rpn) {
        assertNodeType(NodeType.SUB, rpn.removeLast());
    }

    public static void assertMultiply(Deque<Node> rpn) {
        assertNodeType(NodeType.MUL, rpn.removeLast());
    }

    public static void assertDivide(Deque<Node> rpn) {
        assertNodeType(NodeType.DIV, rpn.removeLast());
    }
}
