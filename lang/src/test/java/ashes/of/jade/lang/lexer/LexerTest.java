package ashes.of.jade.lang.lexer;

import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.parser.ParseException;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;


public class LexerTest {


    @Test
    public void testMultilineAssign() {
        String source = "var first = 1\n" +
                        "var second = 2\n" +
                        "var third = first + second\n";

        Lexer lexer = new Lexer();

        List<Lexem> actual = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store, new Location(1, 1), "first"),
                new Lexem(LexemType.IntegerNumber, new Location(1, 13), "1"),
                new Lexem(LexemType.NewLine, new Location(1, 14)),

                new Lexem(LexemType.Store, new Location(2, 1), "second"),
                new Lexem(LexemType.IntegerNumber, new Location(2, 14), "2"),
                new Lexem(LexemType.NewLine, new Location(2, 15)),

                new Lexem(LexemType.Store, new Location(3, 1), "third"),
                new Lexem(LexemType.Load, new Location(3, 13), "first"),
                new Lexem(LexemType.Plus, new Location(3, 19)),
                new Lexem(LexemType.Load, new Location(3, 21), "second"),
                new Lexem(LexemType.NewLine, new Location(3, 27)),
                new Lexem(LexemType.EOF, new Location(4, 1))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void parseShouldThrowAnExceptionIfSymbolIsUnknown() {
        String source = "var first = 1 & 7\n";

        try {
            Lexer lexer = new Lexer();
            lexer.parse(source);
        } catch (ParseException e) {
            assertEquals(new Location(1, 15), e.getLocation());
        }
    }

    @Test
    public void parseShouldNormallyReadNegativeDoubles() {
        String source = "var negative = -1.0\n";

        Lexer lexer = new Lexer();
        List<Lexem> actual = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store, new Location(1, 1), "negative"),
                new Lexem(LexemType.DoubleNumber, new Location(1, 16), "-1.0"),
                new Lexem(LexemType.NewLine, new Location(1, 20)),
                new Lexem(LexemType.EOF, new Location(2, 1))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void parseShouldThrowAnExceptionIfDoubleNumberContainsMoreThanTwoDots() {
        String source = "var negative = -1..0\n";

        try {
            Lexer lexer = new Lexer();
            lexer.parse(source);
        } catch (ParseException e) {
            assertEquals(new Location(1, 16), e.getLocation());
        }
    }

    @Test
    public void parseShouldNormallyReadNegativeIntegers() {
        String source = "var negative = -1\n";

        Lexer lexer = new Lexer();
        List<Lexem> actual = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store, new Location(1, 1), "negative"),
                new Lexem(LexemType.IntegerNumber, new Location(1, 16), "-1"),
                new Lexem(LexemType.NewLine, new Location(1, 18)),
                new Lexem(LexemType.EOF, new Location(2, 1))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void parseShouldNormallyReadNegativeIntegersInExpr() {
        String source = "var negative = 5 - -1";

        Lexer lexer = new Lexer();
        List<Lexem> actual = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store, new Location(1, 1), "negative"),
                new Lexem(LexemType.IntegerNumber, new Location(1, 16), "5"),
                new Lexem(LexemType.Minus, new Location(1, 18)),
                new Lexem(LexemType.IntegerNumber, new Location(1, 20), "-1"),
                // but in real life EOF should be in 1:21...
                new Lexem(LexemType.EOF, new Location(1, 22))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void testSimpleAssignToVarA() {
        String source = "var a = 1";
        Lexer lexer = new Lexer();


        List<Lexem> lexems = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store, new Location(1, 1), "a"),
                new Lexem(LexemType.IntegerNumber, new Location(1, 9), "1"),
                new Lexem(LexemType.EOF, new Location(1, 10))
        );

        assertEquals(expected, lexems);
    }


    @Test
    public void testParseIdentifierWhichStartsFromMap() throws Exception {
        String source = "var mapped = map({0, 1}, e -> e)\n";

        Lexer lexer = new Lexer();
        List<Lexem> lexems = lexer.parse(source);


        System.out.println(lexems);

        Lexem assign = lexems.get(0);
        assertEquals("First should be Store{mapped}", LexemType.Store, assign.getType());
        assertEquals("First should be Store{mapped}", "mapped", assign.getContent());
    }
}