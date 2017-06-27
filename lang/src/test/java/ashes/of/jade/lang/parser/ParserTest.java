package ashes.of.jade.lang.parser;

import ashes.of.jade.lang.lexer.Lexem;
import ashes.of.jade.lang.lexer.Lexer;
import ashes.of.jade.lang.nodes.Node;
import ashes.of.jade.lang.nodes.NodeType;
import org.junit.Before;
import org.junit.Test;

import java.util.Deque;
import java.util.List;

import static ashes.of.jade.lang.NodeAssert.*;
import static org.junit.Assert.*;

public class ParserTest {

    private Lexer lexer;
    private Parser parser;
    
    @Before
    public void setUp() throws Exception {
        lexer = new Lexer();
        parser = new Parser();
    }

    @Test
    public void testAssignExprPlusMinusAndMultiply() {
        String source = "var n = (13 + 6 - 7) * 2\n";

        List<Lexem> lexems = lexer.parse(source);
        Deque<Node> rpn = parser.parse(lexems);

        {
            assertValue(rpn, 13);
            assertValue(rpn, 6);
            assertPlus(rpn);
            assertValue(rpn, 7);
            assertMinus(rpn);
        }
        assertValue(rpn, 2);
        assertMultiply(rpn);

        assertStore(rpn, "n");
    }

    @Test
    public void testAssignExprWithMultiplyAndPlus() {
        String source = "var n = 13 + 6 * 2\n";

        List<Lexem> lexems = lexer.parse(source);
        Deque<Node> rpn = parser.parse(lexems);

        assertValue(rpn, 13);
        {
            assertValue(rpn, 6);
            assertValue(rpn, 2);
            assertMultiply(rpn);
        }
        assertPlus(rpn);


        assertStore(rpn, "n");
    }



    @Test
    public void testAssignSimpleExprWithPlus() {
        String source = "var n = 13 + 6\n";

        List<Lexem> lexems = lexer.parse(source);
        Deque<Node> rpn = parser.parse(lexems);

        assertValue(rpn, 13);
        assertValue(rpn, 6);

        assertPlus(rpn);

        assertStore(rpn, "n");
    }


    @Test
    public void testAssignInteger() {
        String source = "var n = 13\n";

        List<Lexem> lexems = lexer.parse(source);
        Deque<Node> rpn = parser.parse(lexems);


        assertValue(rpn, 13);
        assertStore(rpn, "n");
    }

    @Test
    public void testAssignDouble() {
        String source = "var n = 13.37\n";

        List<Lexem> lexems = lexer.parse(source);
        Deque<Node> rpn = parser.parse(lexems);

        assertValue(rpn, 13.37);
        assertStore(rpn, "n");
    }


    @Test
    public void testSequenceIdInMapParams() throws Exception {
        String source = "var seq = {0, 3}\n" +
                        "var x = map(seq, e -> e * 2)\n";
//                        "var x = map(seq, 5)\n";


        List<Lexem> lexems = lexer.parse(source);
        Deque<Node> rpn = parser.parse(lexems);

        // var seq = {0, 3}
        rpn.removeLast(); // 0
        rpn.removeLast(); // 3
        rpn.removeLast(); // SEQ
        rpn.removeLast(); // STORE seq

        rpn.removeLast(); // NL

        // var mapped = map(seq, e -> e * 2)
        // LOAD seq
        // LAMBDA
        // MAP
        assertNode(NodeType.LOAD, "seq", rpn.removeLast());
        assertNode(NodeType.LAMBDA, rpn.removeLast());

        System.out.println(rpn);
    }



    private void assertStore(Deque<Node> rpn, String var) {
        Node store = rpn.removeLast();
        assertEquals(NodeType.STORE, store.getType());
        assertEquals(var, store.getContent());
    }

    private void assertValue(Deque<Node> rpn, long expected) {
        Node actual = rpn.removeLast();
        assertEquals(NodeType.INTEGER, actual.getType());
        assertEquals(expected, actual.toInteger());
    }

    private void assertValue(Deque<Node> rpn, double expected) {
        Node actual = rpn.removeLast();
        assertEquals(NodeType.DOUBLE, actual.getType());
        assertEquals(expected, actual.toDouble(), 0.0001);
    }
}