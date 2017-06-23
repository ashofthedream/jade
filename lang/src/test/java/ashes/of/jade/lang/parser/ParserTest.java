package ashes.of.jade.lang.parser;

import ashes.of.jade.lang.lexer.Lexem;
import ashes.of.jade.lang.lexer.Lexer;
import ashes.of.jade.lang.nodes.Node;
import ashes.of.jade.lang.nodes.NodeType;
import org.junit.Test;

import java.util.Deque;
import java.util.List;

import static ashes.of.jade.lang.NodeAssert.*;
import static org.junit.Assert.*;

public class ParserTest {


    @Test
    public void testAssignExprPlusMinusAndMultiply() {
        String source = "var n = (13 + 6 - 7) * 2\n";

        Lexer lexer = new Lexer();
        List<Lexem> lexems = lexer.parse(source);

        Parser parser = new Parser();
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

        Lexer lexer = new Lexer();
        List<Lexem> lexems = lexer.parse(source);

        Parser parser = new Parser();
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

        Lexer lexer = new Lexer();
        List<Lexem> lexems = lexer.parse(source);

        Parser parser = new Parser();
        Deque<Node> rpn = parser.parse(lexems);

        assertValue(rpn, 13);
        assertValue(rpn, 6);

        assertPlus(rpn);

        assertStore(rpn, "n");
    }


    @Test
    public void testAssignInteger() {
        String source = "var n = 13\n";


        Lexer lexer = new Lexer();
        List<Lexem> lexems = lexer.parse(source);

        Parser parser = new Parser();
        Deque<Node> rpn = parser.parse(lexems);


        assertValue(rpn, 13);
        assertStore(rpn, "n");
    }

    @Test
    public void testAssignDouble() {
        String source = "var n = 13.37\n";


        Lexer lexer = new Lexer();
        List<Lexem> lexems = lexer.parse(source);

        Parser parser = new Parser();
        Deque<Node> rpn = parser.parse(lexems);

        assertValue(rpn, 13.37);
        assertStore(rpn, "n");
    }


    @Test
    public void testSequenceIdInMapParams() throws Exception {
        String source = "var seq = {0, 3}\n" +
                        "var x = map(seq, e -> e * 2)\n";
//                        "var x = map(seq, 5)\n";

        Lexer lexer = new Lexer();
        List<Lexem> lexems = lexer.parse(source);

        System.out.println(lexems);
        System.out.println();

        Parser parser = new Parser();
        Deque<Node> rpn = parser.parse(lexems);


        System.out.println();
        rpn.stream()
                .map(x -> x.getType() == NodeType.NL ? "^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ \n" : x.toString())
                .forEach(System.out::println);
        System.out.println();

        // var seq = {0, 3} \n
        // 0
        // 3
        // SEQ
        // STORE seq
        rpn.removeLast();
        rpn.removeLast();
        rpn.removeLast();
        rpn.removeLast();

        // NL
        rpn.removeLast();

        // var mapped = map(seq, e -> e * 2)
        // LOAD seq
        // LAMBDA
        // MAP
        assertNode(NodeType.LOAD, "seq", rpn.removeLast());
        assertNode(NodeType.LAMBDA, rpn.removeLast());

        System.out.println(rpn);
    }

    @Test
    public void testParse() {
        String source =
                "var seq = {4, 6}\n" +
                "var sequence = map(seq, i -> i * 2)\n" +
                "var pi = 3.1415 * reduce (sequence, 0, x y -> x + y)\n" +
                "print \"pi = \"\n" +
                "out pi\n" +
                "" ;


        Lexer lexer = new Lexer();
        List<Lexem> lexems = lexer.parse(source);

        Parser parser = new Parser();
        Deque<Node> rpn = parser.parse(lexems);


        System.out.println();
        rpn.stream()
                .map(x -> x.getType() == NodeType.NL ? "^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ \n" : x.toString())
                .forEach(System.out::println);
        System.out.println();
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