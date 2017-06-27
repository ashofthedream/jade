package ashes.of.jade.lang;

import ashes.of.jade.lang.interpreter.EvalException;
import ashes.of.jade.lang.interpreter.Interpreter;
import ashes.of.jade.lang.interpreter.Interpreter.Scope;
import ashes.of.jade.lang.lexer.Lexer;
import ashes.of.jade.lang.nodes.IntegerSeqNode;
import ashes.of.jade.lang.nodes.Node;
import ashes.of.jade.lang.parser.Parser;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InterpreterTest {

    private Interpreter interpreter;

    
    @Before
    public void setUp() throws Exception {
        interpreter = new Interpreter(new Lexer(), new Parser());
    }


    /*
     * operators
     */

    @Test
    public void evalShouldReturnStateAfterEvaluateExprWithAllOperatorsAndParenthesis() throws Exception {
        Scope scope = interpreter.eval("var a = 5 + 2 ^ 4 / 8 * (10 - 5)");

        Node a = scope.load("a");

        assertTrue(a.isInteger());
        assertEquals("5 + 2 ^ 4 / 8 * (10 - 5)", 15, a.toInteger());
    }


    @Test
    public void evalShouldReturnStateAfterEvaluateExprWithPlusAndMultiply() throws Exception {
        Scope scope = interpreter.eval("var a = 5 + 2 * 2");

        Node a = scope.load("a");

        assertTrue(a.isInteger());
        assertEquals("5 + 2 * 2", 9, a.toInteger());
    }

    @Test
    public void evalShouldReturnStateAfterEvaluateExprWithMinusInParenthesesAndDivide() throws Exception {
        Scope scope = interpreter.eval("var a = (5 - 2) / 3");

        Node a = scope.load("a");

        assertTrue(a.isInteger());
        assertEquals("(5 - 2) / 3", 1, a.toInteger());
    }


    @Test
    public void evalShouldReturnStateAfterEvaluateExprWithPowerAndMultiplyWithoutParenthesis() throws Exception {
        Scope scope = interpreter.eval("var a = 5 ^ 2 * 2");

        Node a = scope.load("a");

        assertTrue(a.isInteger());
        assertEquals("5 ^ 2 * 2", 50, a.toInteger());
    }


    @Test
    public void evalShouldReturnStateAfterEvaluateExprWithPowerAndMultiplyWithParenthesis() throws Exception {
        Scope scope = interpreter.eval("var a = 5 ^ (2 * 2)");

        Node a = scope.load("a");

        assertTrue(a.isInteger());
        assertEquals(" 5 ^ (2 * 2)", 625, a.toInteger());
    }

    @Test
    public void evalShouldReturnStateAfterEvaluateExprWithPowerAndDivideWithoutParenthesis() throws Exception {
        Scope scope = interpreter.eval("var a = 2 ^ 4 / 8");

        Node a = scope.load("a");

        assertTrue(a.isInteger());
        assertEquals("2 ^ 4 / 8", 2, a.toInteger());
    }


    @Test(expected = EvalException.class)
    public void evalShouldThrowAnExceptionIfInputContainsNumberAndSequenceExpression() throws Exception {
        interpreter.eval("var x = 5 + {0, 100}");
    }


    /*
     * seq
     */
    @Test
    public void evalShouldEvaluateSequenceCreateIfMinAndMaxIsExpressions() throws Exception {
        String source = "var seq = {0 * 5, 5 + 5}";
        Scope scope = interpreter.eval(source);

        IntegerSeqNode seq = scope.load("seq")
                .toIntegerSeq();

        assertArrayEquals("{0 * 5, 5 + 5} -> 0..10",
                new long[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, seq.seq);
    }

    @Test
    public void evalShouldEvaluateSequenceCreateIfMinAndMaxIsExpressionsWithMapReduce() throws Exception {
        String source = "var seq = {reduce(map({0, 1}, x -> x + x), 0, a b -> a + b), reduce(map({0, 3}, x -> x + x), 0, a b -> a + b)}";
        Scope scope = interpreter.eval(source);

        IntegerSeqNode seq = scope.load("seq")
                .toIntegerSeq();

        assertArrayEquals("{reduced 2, reduced 12} -> 2..12",
                new long[] {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12}, seq.seq);
    }


    /*
     * print
     */

    @Test(expected = EvalException.class)
    public void evalShouldThrowAnExceptionIfPrintArgumentsIsNotString() throws Exception {
        interpreter.eval("print 5 + 2");
    }


    @Test(expected = EvalException.class)
    public void evalShouldSinkOUTToOutputStream() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(baos);

        interpreter.setOut(stream);
        interpreter.eval("print 5 + 2");

        String out = baos.toString(Charset.defaultCharset().name());

        assertEquals("print 5 + 2", "7", out);
    }

    /*
     * out
     */

    @Test(expected = EvalException.class)
    public void evalShouldThrowAnExceptionIfOutArgumentIsString() throws Exception {
        interpreter.eval("out \"ahaha it's a string\"");
    }

    /*
     * map
     */

    @Test(expected = EvalException.class)
    public void evalShouldThrowAnExceptionIfInputContainsStringFirstMapParameter() throws Exception {
        interpreter.eval("map(\"this is a string\", x -> x)");
    }

    @Test(expected = EvalException.class)
    public void evalShouldThrowAnExceptionIfInputContainsIntegerFirstMapParameter() throws Exception {
        interpreter.eval("var m = map(13, x -> x)");
    }

    @Test(expected = EvalException.class)
    public void evalShouldThrowAnExceptionIfInputContainsDoubleFirstMapParameter() throws Exception {
        interpreter.eval("var m = map(13.37, x -> x)");
    }
    
    @Test
    public void evalShouldReturnStateAfterEvaluateMapExpr() throws Exception {
        Scope scope = interpreter.eval("var seq = map({0, 5}, x -> x * x)");

        Node a = scope.load("seq");

        assertTrue(a.isIntegerSeq());
        assertArrayEquals("0..5 -> 0..25", new long[] {0, 1, 4, 9, 16, 25}, a.toIntegerSeq().seq);
    }

    @Test
    public void evalShouldReturnStateAfterEvaluateMapExprWithIntToDoubleConversion() throws Exception {
        Scope scope = interpreter.eval("var seq = map({0, 5}, x -> x * x * 1.0)");

        Node a = scope.load("seq");

        assertTrue(a.isDoubleSeq());
        assertArrayEquals("0..5 -> 0..25", new double[] {0., 1., 4., 9., 16., 25.}, a.toDoubleSeq().seq, 0.0001);
    }


    /*
     * reduce
     */

    @Test
    public void evalShouldEvaluateMapReduceInOneLineExpression() throws Exception {
        String source = "var reduced = reduce(map({1, 5}, x -> x * x), 0, a b -> a + b)";
        Scope scope = interpreter.eval(source);

        Node a = scope.load("reduced");

        assertTrue(a.isInteger());
        assertEquals("reduced = ", 55, a.toInteger());
    }
}