package ashes.of.jade.lang;

import ashes.of.jade.lang.interpreter.EvalException;
import ashes.of.jade.lang.interpreter.Interpreter;
import ashes.of.jade.lang.lexer.Lexem;
import ashes.of.jade.lang.lexer.Lexer;
import ashes.of.jade.lang.nodes.Node;
import ashes.of.jade.lang.parser.Parser;
import org.junit.Test;

import java.util.Deque;
import java.util.List;

public class InterpreterTest {


    @Test(expected = EvalException.class)
    public void evalShouldThrowAnExceptionIfSourceCodeContainsNumberAndSequenceExpression() throws Exception {
        String source = "var x = 5 + {0, 100}";

        Lexer lexer = new Lexer();
        List<Lexem> lexems = lexer.parse(source);

        Parser parser = new Parser();
        Deque<Node> nodes = parser.parse(lexems);

        Interpreter interpreter = new Interpreter(lexer, parser);
        interpreter.eval(nodes);
    }

    @Test(expected = EvalException.class)
    public void evalShouldThrowAnExceptionIfSourceCodeContainsIntegerFirstMapParameter() throws Exception {
        String source = "var m = map(13, x -> x)";

        Lexer lexer = new Lexer();
        List<Lexem> lexems = lexer.parse(source);

        Parser parser = new Parser();
        Deque<Node> nodes = parser.parse(lexems);

        Interpreter interpreter = new Interpreter(lexer, parser);
        interpreter.eval(nodes);
    }

    @Test(expected = EvalException.class)
    public void evalShouldThrowAnExceptionIfSourceCodeContainsDoubleFirstMapParameter() throws Exception {
        String source = "var m = map(13.37, x -> x)";

        Lexer lexer = new Lexer();
        List<Lexem> lexems = lexer.parse(source);

        Parser parser = new Parser();
        Deque<Node> nodes = parser.parse(lexems);

        Interpreter interpreter = new Interpreter(lexer, parser);
        interpreter.eval(nodes);
    }


    @Test(expected = EvalException.class)
    public void evalShouldThrowAnExceptionIfPrintArgumentsIsNotString() throws Exception {
        String source = "print 5 + 2";

        Lexer lexer = new Lexer();
        List<Lexem> lexems = lexer.parse(source);

        Parser parser = new Parser();
        Deque<Node> nodes = parser.parse(lexems);

        Interpreter interpreter = new Interpreter(lexer, parser);
        interpreter.eval(nodes);
    }


    @Test(expected = EvalException.class)
    public void evalShouldThrowAnExceptionIfOutArgumentIsString() throws Exception {
        String source = "out \"ahaha it's a string\"";

        Lexer lexer = new Lexer();
        List<Lexem> lexems = lexer.parse(source);

        Parser parser = new Parser();
        Deque<Node> nodes = parser.parse(lexems);

        Interpreter interpreter = new Interpreter(lexer, parser);
        interpreter.eval(nodes);
    }


    @Test(expected = EvalException.class)
    public void evalShouldThrowAnExceptionIfSourceCodeContainsStringFirstMapParameter() throws Exception {
        String source = "map(\"this is a string\", x -> x)";

        Lexer lexer = new Lexer();
        List<Lexem> lexems = lexer.parse(source);

        Parser parser = new Parser();
        Deque<Node> nodes = parser.parse(lexems);

        Interpreter interpreter = new Interpreter(lexer, parser);
        interpreter.eval(nodes);
    }    
}