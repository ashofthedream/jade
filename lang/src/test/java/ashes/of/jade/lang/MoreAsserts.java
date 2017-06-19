package ashes.of.jade.lang;

import java.util.Deque;

import static org.junit.Assert.assertEquals;

public class MoreAsserts {



    public static void assertPlus(Deque<Node> rpn) {
        Node n = rpn.removeLast();
        assertEquals(LexemType.Plus, n.getType());
    }

    public static void assertMinus(Deque<Node> rpn) {
        Node n = rpn.removeLast();
        assertEquals(LexemType.Minus, n.getType());
    }

    public static void assertMultiply(Deque<Node> rpn) {
        Node n = rpn.removeLast();
        assertEquals(LexemType.Multiply, n.getType());
    }

    public static void assertDivide(Deque<Node> rpn) {
        Node n = rpn.removeLast();
        assertEquals(LexemType.Divide, n.getType());
    }    
}
