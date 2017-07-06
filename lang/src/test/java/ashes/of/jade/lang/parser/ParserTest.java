package ashes.of.jade.lang.parser;

import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.interpreter.EvalException;
import ashes.of.jade.lang.lexer.Lexem;
import ashes.of.jade.lang.lexer.Lexer;
import ashes.of.jade.lang.lexer.LexerTest;
import ashes.of.jade.lang.nodes.Node;
import ashes.of.jade.lang.nodes.NodeType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.util.Deque;
import java.util.List;

import static ashes.of.jade.lang.NodeAssert.*;
import static org.junit.Assert.*;

public class ParserTest {
    private static final Logger log = LogManager.getLogger(LexerTest.class);

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
        String source = "var n = 13 * 6 ^ 2\n";

        List<Lexem> lexems = lexer.parse(source);
        Deque<Node> rpn = parser.parse(lexems);

        assertValue(rpn, 13);
        {
            assertValue(rpn, 6);
            assertValue(rpn, 2);
            assertPower(rpn);
        }
        assertMultiply(rpn);


        assertStore(rpn, "n");
    }


    @Test
    public void testAssignExprWithMultiplyAndPower() {
        String source = "var n = 13 + 6 ^ 2\n";

        List<Lexem> lexems = lexer.parse(source);
        Deque<Node> rpn = parser.parse(lexems);

        assertValue(rpn, 13);
        {
            assertValue(rpn, 6);
            assertValue(rpn, 2);
            assertPower(rpn);
        }
        assertPlus(rpn);


        assertStore(rpn, "n");
    }

    @Test
    public void testAssignExprWithDivideInParenthesisAndPower() {
        String source = "var n = (15 / 4.0) ^ 2\n";

        List<Lexem> lexems = lexer.parse(source);
        Deque<Node> rpn = parser.parse(lexems);

        {
            assertValue(rpn, 15);
            assertValue(rpn, 4.0);
            assertDivide(rpn);
        }
        assertValue(rpn, 2);
        assertPower(rpn);


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
    public void parseShouldThrowAnExceptionIfExpressionContainsTwoIntegersWithoutOperation() {
        try {
            List<Lexem> lexems = lexer.parse("var a = 2 4");
            Deque<Node> rpn = parser.parse(lexems);

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(10, 1, 11), e.getLocation());
        }
    }

    @Test
    public void parseShouldThrowAnExceptionIfExpressionContainsTwoDoublesWithoutOperation() {
        try {
            List<Lexem> lexems = lexer.parse("var a = 2.0 0.4");
            Deque<Node> rpn = parser.parse(lexems);

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(12, 1, 13), e.getLocation());
        }
    }


    @Test
    public void testAssignDouble() {
        List<Lexem> lexems = lexer.parse("var n = 13.37\n");
        Deque<Node> rpn = parser.parse(lexems);

        assertValue(rpn, 13.37);
        assertStore(rpn, "n");
    }

    @Test
    public void parserShouldThrowAnExceptionIfSeqDeclarationEndsWithWrongBrace() {
        try {
            List<Lexem> lexems = lexer.parse("var seq = {-1, 2)");
            Deque<Node> rpn = parser.parse(lexems);

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(16, 1, 17), e.getLocation());
        }
    }

    @Test
    public void parserShouldThrowAnExceptionIfSeqDeclarationEndsWithWrongOpenCurlyBrace() {
        try {
            List<Lexem> lexems = lexer.parse("var seq = {0, 2{");
            Deque<Node> rpn = parser.parse(lexems);

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(16, 1, 17), e.getLocation());
        }
    }



    @Test
    public void parseShouldFailIfSequenceContainsOnlyOneExpr() {
        try {
            List<Lexem> lexems = lexer.parse("var seq = {2}");
            Deque<Node> rpn = parser.parse(lexems);

            log.error("Parser should fail, ut output is: {}", rpn);
            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(12, 1, 13), e.getLocation());
        }
    }

    @Test
    public void parseShouldFailIfSequenceEndsWithoutStart() {
        try {
            List<Lexem> lexems = lexer.parse("var seq = {0, 2}}");
            Deque<Node> rpn = parser.parse(lexems);

            log.error("Parser should fail, ut output is: {}", rpn);
            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(16, 1, 17), e.getLocation());
        }
    }


    @Test
    public void parseShouldThrowAnExceptionIfExpressionContainsTwoSeqWithoutOperation() {
        try {
            List<Lexem> lexems = lexer.parse("var seq = {0, 2} {0, 2}");
            Deque<Node> rpn = parser.parse(lexems);

            log.error("Parser should fail, ut output is: {}", rpn);
            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(17, 1, 18), e.getLocation());
        }
    }



    @Test
    public void parseShouldThrowAnExceptionIfExpressionContainsTwoStringsWithoutOperation() {
        try {
            List<Lexem> lexems = lexer.parse("print \"hello\" \"world\"");
            Deque<Node> rpn = parser.parse(lexems);

            log.error("Parser should fail, ut output is: {}", rpn);
            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(14, 1, 15), e.getLocation());
        }
    }

    @Test
    public void parseShouldReturnValidStackIfOutContainsOneInteger() {
        List<Lexem> lexems = lexer.parse("out 13\n");
        Deque<Node> rpn = parser.parse(lexems);

        assertValue(rpn, 13);
        assertNode(rpn, NodeType.OUT);
    }


    @Test
    public void parseShouldFailIfOutExprContainsMoreThanOneVariables() {
        try {
            List<Lexem> lexems = lexer.parse(
                    "var a = 1\n" +
                    "var b = 2\n" +
                    "out a b");
            Deque<Node> rpn = parser.parse(lexems);

            fail("Eval should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(26, 3, 7), e.getLocation());
        }
    }

    @Test
    public void parseShouldFailIfOutExprContainsMoreThanOneInteger() {
        try {
            List<Lexem> lexems = lexer.parse("out 7 2");
            Deque<Node> rpn = parser.parse(lexems);

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(6, 1, 7), e.getLocation());
        }
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
        rpn.removeLast(); // NEWSEQUENCE
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
}