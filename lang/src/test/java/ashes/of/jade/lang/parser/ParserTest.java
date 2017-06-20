package ashes.of.jade.lang.parser;

import ashes.of.jade.lang.lexer.Lexem;
import ashes.of.jade.lang.lexer.Lexer;
import ashes.of.jade.lang.nodes.Node;
import ashes.of.jade.lang.nodes.NodeType;
import org.junit.Test;

import java.util.Deque;
import java.util.List;

import static ashes.of.jade.lang.NodeAssert.assertMinus;
import static ashes.of.jade.lang.NodeAssert.assertMultiply;
import static ashes.of.jade.lang.NodeAssert.assertPlus;
import static org.junit.Assert.*;

public class ParserTest {


    @Test
    public void testAssignExprPlusMinusAndMultiply() {
        String source = "var n = (13 + 6 - 7) * 2\n";

        Lexer lexer = new Lexer();
        List<Lexem> lexems = lexer.parse(source);

        Parser parser = new Parser(lexems);
        Deque<Node> rpn = parser.parse();

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

        Parser parser = new Parser(lexems);
        Deque<Node> rpn = parser.parse();

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

        Parser parser = new Parser(lexems);
        Deque<Node> rpn = parser.parse();

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

        Parser parser = new Parser(lexems);
        Deque<Node> rpn = parser.parse();


        assertValue(rpn, 13);
        assertStore(rpn, "n");
    }

    @Test
    public void testAssignDouble() {
        String source = "var n = 13.37\n";


        Lexer lexer = new Lexer();
        List<Lexem> lexems = lexer.parse(source);

        Parser parser = new Parser(lexems);
        Deque<Node> rpn = parser.parse();

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

        Parser parser = new Parser(lexems);
        Deque<Node> rpn = parser.parse();


        System.out.println();
        rpn.stream()
                .map(x -> x.getType() == NodeType.NL ? "^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ ^ \n" : x.toString())
                .forEach(System.out::println);
        System.out.println();

        // var seq = {0, 3} \n
        rpn.removeLast();
        rpn.removeLast();
        rpn.removeLast();
        rpn.removeLast();

        rpn.removeLast();

        // var mapped = map(seq, e -> e * 2)
        Node seq = rpn.removeLast();
        Node lambda = rpn.removeLast();

        assertEquals(NodeType.LOAD, seq.getType());
        assertEquals("seq", seq.getContent());

        assertEquals(NodeType.LAMBDA, lambda.getType());

        System.out.println(seq);
        System.out.println(lambda);
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

        Parser parser = new Parser(lexems);
        Deque<Node> rpn = parser.parse();


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