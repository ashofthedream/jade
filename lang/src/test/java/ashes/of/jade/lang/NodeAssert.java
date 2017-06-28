package ashes.of.jade.lang;

import ashes.of.jade.lang.nodes.Node;
import ashes.of.jade.lang.nodes.NodeType;

import java.util.Deque;

import static org.junit.Assert.assertEquals;

public class NodeAssert {



    public static void assertNodeType(NodeType expected, Node n) {
        assertEquals(expected, n.getType());
    }


    public static void assertNode(Deque<Node> rpn, NodeType type) {
        assertNodeType(type, rpn.removeLast());
    }

    public static void assertNode(NodeType expected, Node n) {
        assertEquals(expected, n.getType());
    }

    public static void assertNode(NodeType expected, String content, Node n) {
        assertEquals(expected, n.getType());
        assertEquals(content, n.getContent());
    }


    public static void assertPlus(Deque<Node> rpn) {
        assertNode(rpn, NodeType.ADD);
    }

    public static void assertMinus(Deque<Node> rpn) {
        assertNode(rpn, NodeType.SUB);
    }

    public static void assertMultiply(Deque<Node> rpn) {
        assertNode(rpn, NodeType.MUL);
    }

    public static void assertDivide(Deque<Node> rpn) {
        assertNode(rpn, NodeType.DIV);
    }

    public static void assertPower(Deque<Node> rpn) {
        assertNode(rpn, NodeType.POWER);
    }


    public static void assertStore(Deque<Node> rpn, String var) {
        Node store = rpn.removeLast();
        assertEquals(NodeType.STORE, store.getType());
        assertEquals(var, store.getContent());
    }

    public static void assertValue(Deque<Node> rpn, long expected) {
        Node actual = rpn.removeLast();
        assertEquals(NodeType.INTEGER, actual.getType());
        assertEquals(expected, actual.toInteger());
    }

    public static void assertValue(Deque<Node> rpn, double expected) {
        Node actual = rpn.removeLast();
        assertEquals(NodeType.DOUBLE, actual.getType());
        assertEquals(expected, actual.toDouble(), 0.0001);
    }
}
