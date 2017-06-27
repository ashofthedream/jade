package ashes.of.jade.lang.lexer;

import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.parser.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;


public class LexerTest {
    private static final Logger log = LogManager.getLogger(LexerTest.class);


    private Lexer lexer;

    @Before
    public void setUp() throws Exception {
        lexer = new Lexer();
    }

    @Test
    public void parseShouldReturnOnlyEofIfInputIsEmpty() {
        List<Lexem> actual = lexer.parse("");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.EOF, new Location(0, 1, 1))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void testMultilineAssign() {
        List<Lexem> actual = lexer.parse(
                "var first = 1\n" +
                "var second = 2\n" +
                "var third = first + second");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store,          new Location(0, 1, 1), "first"),
                new Lexem(LexemType.IntegerNumber,  new Location(12, 1, 13), "1"),
                new Lexem(LexemType.NewLine,        new Location(13, 1, 14)),

                new Lexem(LexemType.Store,          new Location(14, 2, 1), "second"),
                new Lexem(LexemType.IntegerNumber,  new Location(27, 2, 14), "2"),
                new Lexem(LexemType.NewLine,        new Location(28, 2, 15)),

                new Lexem(LexemType.Store,          new Location(29, 3, 1), "third"),
                new Lexem(LexemType.Load,           new Location(41, 3, 13), "first"),
                new Lexem(LexemType.Plus,           new Location(47, 3, 19)),
                new Lexem(LexemType.Load,           new Location(49, 3, 21), "second"),
                new Lexem(LexemType.EOF,            new Location(55, 3, 27))
        );

        assertEquals(expected, actual);
    }


    @Test
    public void parseShouldThrowAnExceptionIfLineStartsFromPlus() {
        try {
            lexer.parse("+10\n");

            fail("Parse should fail");
        } catch (ParseException e) {
            assertEquals(new Location(0, 1, 1), e.getLocation());
        }
    }

    @Test
    public void parseShouldThrowAnExceptionIfLineStartsFromMinus() {
        try {
            lexer.parse("-10\n");

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(0, 1, 1), e.getLocation());
        }
    }

    @Test
    public void parseShouldThrowAnExceptionIfLineStartsFromString() {
        try {
            lexer.parse("\"hello world\"\n");

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(0, 1, 1), e.getLocation());
        }
    }

    @Test
    public void parseShouldThrowAnExceptionIfLineStartsFromInteger() {
        try {
            lexer.parse("13 + 37\n");

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(0, 1, 1), e.getLocation());
        }
    }

    @Test
    public void parseShouldThrowAnExceptionIfLineStartsFromDouble() {
        try {
            lexer.parse("13.37\n");

            fail("Parse should fail");
        } catch (ParseException e) {
            assertEquals(new Location(0, 1, 1), e.getLocation());
        }
    }

    @Test
    public void parseShouldThrowAnExceptionIfLineStartsFromIdentifier() {
        try {
            lexer.parse("a = 1\n");

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(0, 1, 1), e.getLocation());
        }
    }

    @Test
    public void parseShouldThrowAnExceptionIfSecondLineStartsFromIdentifier() {
        try {
            lexer.parse("var a = 5\n" +
                        "b = 1\n");

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(10, 2, 1), e.getLocation());
        }
    }


    @Test
    public void parseShouldThrowAnExceptionIfSymbolIsUnknown() {
        try {
            lexer.parse("var first = 1 & 7\n");

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(14, 1, 15), e.getLocation());
        }
    }

    @Test
    public void parseShouldNormallyReadPositiveDoubles() {
        List<Lexem> actual = lexer.parse("var positive = +1.0\n");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store,          new Location(0, 1, 1), "positive"),
                new Lexem(LexemType.DoubleNumber,   new Location(15, 1, 16), "+1.0"),
                new Lexem(LexemType.NewLine,        new Location(19, 1, 20)),
                new Lexem(LexemType.EOF,            new Location(20, 2, 1))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void parseShouldNormallyReadNegativeDoubles() {
        List<Lexem> actual = lexer.parse("var negative = -1.0\n");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store,          new Location(0, 1, 1), "negative"),
                new Lexem(LexemType.DoubleNumber,   new Location(15, 1, 16), "-1.0"),
                new Lexem(LexemType.NewLine,        new Location(19, 1, 20)),
                new Lexem(LexemType.EOF,            new Location(20, 2, 1))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void parseShouldThrowAnExceptionIfDoubleNumberContainsMoreThanTwoDots() {
        try {
            lexer.parse("var negative = -1..0\n");

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(15, 1, 16), e.getLocation());
        }
    }

    @Test
    public void parseShouldNormallyReadPositiveIntegers() {
        List<Lexem> actual = lexer.parse("var positive = +1");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store,          new Location(0, 1, 1), "positive"),
                new Lexem(LexemType.IntegerNumber,  new Location(15, 1, 16), "+1"),
                new Lexem(LexemType.EOF,            new Location(17, 1, 18))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void parseShouldNormallyReadNegativeIntegers() {
        List<Lexem> actual = lexer.parse("var negative = -1");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store,          new Location(0, 1, 1), "negative"),
                new Lexem(LexemType.IntegerNumber,  new Location(15, 1, 16), "-1"),
                new Lexem(LexemType.EOF,            new Location(17, 1, 18))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void parseShouldThrowAnExceptionIfAArrowStartsWithIllegalToken() {
        String sources[] = {
                "out map({0, 1}, x +> x + 1)",
                "out map({0, 1}, x => x + 1)",
                "out map({0, 1}, x /> x + 1)",
                "out map({0, 1}, x l> x + 1)",
                "out map({0, 1}, x ^> x + 1)",
        };

        for (String source : sources) {
            try {
                lexer.parse(source);

                fail("Parse should fail");
            } catch (ParseException e) {
                log.warn("Can't parse", e);
                assertEquals(new Location(18, 1, 19), e.getLocation());
            }
        }

    }

    @Test
    public void parseShouldNormallyReadEmptyStrings() {
        List<Lexem> actual = lexer.parse("var empty = \"\"");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store,          new Location(0, 1, 1), "empty"),
                new Lexem(LexemType.String,         new Location(12, 1, 13), ""),
                new Lexem(LexemType.EOF,            new Location(14, 1, 15))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void parseShouldNormallyReadNonEmptyStrings() {
        List<Lexem> actual = lexer.parse("var str = \"this is a string\"");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store,          new Location(0, 1, 1), "str"),
                new Lexem(LexemType.String,         new Location(10, 1, 11), "this is a string"),
                new Lexem(LexemType.EOF,            new Location(28, 1, 29))
        );

        assertEquals(expected, actual);
    }


    @Test
    public void parseShouldNormallyReadNonEmptyStringsWithEscapedDoubleQuotes() {
        List<Lexem> actual = lexer.parse("var str = \"a \\\"string\\\" with escaped double quotes\"");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store,          new Location(0, 1, 1), "str"),
                new Lexem(LexemType.String,         new Location(10, 1, 11), "a \"string\" with escaped double quotes"),
                new Lexem(LexemType.EOF,            new Location(51, 1, 52))
        );

        assertEquals(expected, actual);
    }


    @Test
    public void parseShouldThrowAnExceptionIfStringIsNotEndsWithDoubleQuote() {

        try {
    
            lexer.parse("var str = \"a string...");

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(10, 1, 11), e.getLocation());
        }
    }

    @Test
    public void parseShouldNormallyReadNegativeIntegersInExpr() {
        List<Lexem> actual = lexer.parse("var negative = 5 - -1");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store,          new Location(0, 1, 1), "negative"),
                new Lexem(LexemType.IntegerNumber,  new Location(15, 1, 16), "5"),
                new Lexem(LexemType.Minus,          new Location(17, 1, 18)),
                new Lexem(LexemType.IntegerNumber,  new Location(19, 1, 20), "-1"),
                // but in real life EOF should be in 1:21...
                new Lexem(LexemType.EOF,            new Location(21, 1, 22))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void parseShouldNormallyReadPositiveIntegersInExpr() {
        List<Lexem> actual = lexer.parse("var positive = 5 - +1");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store,          new Location(0, 1, 1), "positive"),
                new Lexem(LexemType.IntegerNumber,  new Location(15, 1, 16), "5"),
                new Lexem(LexemType.Minus,          new Location(17, 1, 18)),
                new Lexem(LexemType.IntegerNumber,  new Location(19, 1, 20), "+1"),
                // but in real life EOF should be in 1:21...
                new Lexem(LexemType.EOF,            new Location(21, 1, 22))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void parseShouldThrowAnExceptionIfMultiplyOperatorIsSecondInExpr() {
        try {
            lexer.parse("var a = 5 - * 1");

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(12, 1, 13), e.getLocation());
        }
    }

    @Test
    public void parseShouldThrowAnExceptionIfDivideOperatorIsSecondInExpr() {
        try {
            lexer.parse("var a = 5 - / 1");

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(12, 1, 13), e.getLocation());
        }
    }



    @Test
    public void parseShouldThrowAnExceptionIfPowerOperatorIsSecondInExpr() {
        try {
            lexer.parse("var a = 5 - ^ 1");

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(12, 1, 13), e.getLocation());
        }
    }

    @Test
    public void parseShouldReturnListOfLexemsIsInputIsOnlyMap() {
        List<Lexem> lexems = lexer.parse("var a = map({0, 1}, e -> e)");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store,          new Location(0, 1, 1), "a"),
                new Lexem(LexemType.Map,            new Location(8, 1, 9)),
                new Lexem(LexemType.ParentOpen,     new Location(11, 1, 12)),
                new Lexem(LexemType.CurlyOpen,      new Location(12, 1, 13)),
                new Lexem(LexemType.IntegerNumber,  new Location(13, 1, 14), "0"),
                new Lexem(LexemType.Comma,          new Location(14, 1, 15)),
                new Lexem(LexemType.IntegerNumber,  new Location(16, 1, 17), "1"),
                new Lexem(LexemType.CurlyClose,     new Location(17, 1, 18)),
                new Lexem(LexemType.Comma,          new Location(18, 1, 19)),

                new Lexem(LexemType.Load,           new Location(20, 1, 21), "e"),
                new Lexem(LexemType.Arrow,          new Location(22, 1, 23)),
                new Lexem(LexemType.Load,           new Location(25, 1, 26), "e"),
                new Lexem(LexemType.ParentClose,    new Location(26, 1, 27)),
                new Lexem(LexemType.EOF,            new Location(27, 1, 28))
        );

        assertEquals(expected, lexems);
    }



    @Test
    public void parseShouldReturnListOfLexemsIsInputIsOnlyOutWithString() {
        List<Lexem> lexems = lexer.parse("print \"hello wold\"");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Print,          new Location(0, 1, 1)),
                new Lexem(LexemType.String,         new Location(6, 1, 7), "hello wold"),
                new Lexem(LexemType.EOF,            new Location(18, 1, 19))
        );

        assertEquals(expected, lexems);
    }

    @Test
    public void parseShouldReturnListOfLexemsIsInputIsOnlyOutWithNumber() {
        List<Lexem> lexems = lexer.parse("out 1");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Out,            new Location(0, 1, 1)),
                new Lexem(LexemType.IntegerNumber,  new Location(4, 1, 5), "1"),
                new Lexem(LexemType.EOF,            new Location(5, 1, 6))
        );

        assertEquals(expected, lexems);
    }

    @Test
    public void parseShouldReturnListOfLexemsIsInputIsOnlyReduce() {
        List<Lexem> lexems = lexer.parse("var a = reduce({0, 1}, -7, a b -> a + b)");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store,          new Location(0, 1, 1), "a"),
                new Lexem(LexemType.Reduce,         new Location(8, 1, 9)),
                new Lexem(LexemType.ParentOpen,     new Location(14, 1, 15)),
                new Lexem(LexemType.CurlyOpen,      new Location(15, 1, 16)),
                new Lexem(LexemType.IntegerNumber,  new Location(16, 1, 17), "0"),
                new Lexem(LexemType.Comma,          new Location(17, 1, 18)),
                new Lexem(LexemType.IntegerNumber,  new Location(19, 1, 20), "1"),
                new Lexem(LexemType.CurlyClose,     new Location(20, 1, 21)),
                new Lexem(LexemType.Comma,          new Location(21, 1, 22)),
                new Lexem(LexemType.IntegerNumber,  new Location(23, 1, 24), "-7"),
                new Lexem(LexemType.Comma,          new Location(25, 1, 26)),

                new Lexem(LexemType.Load,           new Location(27, 1, 28), "a"),
                new Lexem(LexemType.Load,           new Location(29, 1, 30), "b"),
                new Lexem(LexemType.Arrow,          new Location(31, 1, 32)),
                new Lexem(LexemType.Load,           new Location(34, 1, 35), "a"),
                new Lexem(LexemType.Plus,           new Location(36, 1, 37)),
                new Lexem(LexemType.Load,           new Location(38, 1, 39), "b"),
                new Lexem(LexemType.ParentClose,    new Location(39, 1, 40)),
                new Lexem(LexemType.EOF,            new Location(40, 1, 41))
        );

        assertEquals(expected, lexems);
    }

    @Test
    public void parseShouldReturnValidOutputIfInputIsAssignSequenceToSeqVar() {
        List<Lexem> lexems = lexer.parse("var seq = {0, 1}");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store,          new Location(0, 1, 1), "seq"),
                new Lexem(LexemType.CurlyOpen,      new Location(10, 1, 11)),
                new Lexem(LexemType.IntegerNumber,  new Location(11, 1, 12), "0"),
                new Lexem(LexemType.Comma,          new Location(12, 1, 13)),
                new Lexem(LexemType.IntegerNumber,  new Location(14, 1, 15), "1"),
                new Lexem(LexemType.CurlyClose,     new Location(15, 1, 16)),
                new Lexem(LexemType.EOF,            new Location(16, 1, 17))
        );

        assertEquals(expected, lexems);
    }


    @Test
    public void parseShouldReturnValidOutputIfInputIsAssignSequenceWithNegativeNumbersToSeqVar() {
        List<Lexem> lexems = lexer.parse("var seq = {-1, 1}");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store,          new Location(0, 1, 1), "seq"),
                new Lexem(LexemType.CurlyOpen,      new Location(10, 1, 11)),
                new Lexem(LexemType.IntegerNumber,  new Location(11, 1, 12), "-1"),
                new Lexem(LexemType.Comma,          new Location(13, 1, 14)),
                new Lexem(LexemType.IntegerNumber,  new Location(15, 1, 16), "1"),
                new Lexem(LexemType.CurlyClose,     new Location(16, 1, 17)),
                new Lexem(LexemType.EOF,            new Location(17, 1, 18))
        );

        assertEquals(expected, lexems);
    }

    @Test
    public void parseShouldReturnListOfLexemsIfInputIsOutWithPlusExpr() {
        List<Lexem> lexems = lexer.parse("out -5 + 8");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Out,            new Location(0, 1, 1)),
                new Lexem(LexemType.IntegerNumber,  new Location(4, 1, 5), "-5"),
                new Lexem(LexemType.Plus,           new Location(7, 1, 8)),
                new Lexem(LexemType.IntegerNumber,  new Location(9, 1, 10), "8"),
                new Lexem(LexemType.EOF,            new Location(10, 1, 11))
        );

        assertEquals(expected, lexems);
    }


    @Test
    public void parseShouldReturnListOfLexemsIfInputIsOutWithMinusExpr() {
        List<Lexem> lexems = lexer.parse("out 5 - 3");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Out,            new Location(0, 1, 1)),
                new Lexem(LexemType.IntegerNumber,  new Location(4, 1, 5), "5"),
                new Lexem(LexemType.Minus,          new Location(6, 1, 7)),
                new Lexem(LexemType.IntegerNumber,  new Location(8, 1, 9), "3"),
                new Lexem(LexemType.EOF,            new Location(9, 1, 10))
        );

        assertEquals(expected, lexems);
    }

    @Test
    public void parseShouldReturnListOfLexemsIfInputIsOutWithMultiplyExpr() {
        List<Lexem> lexems = lexer.parse("out 5 * 1");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Out,            new Location(0, 1, 1)),
                new Lexem(LexemType.IntegerNumber,  new Location(4, 1, 5), "5"),
                new Lexem(LexemType.Multiply,       new Location(6, 1, 7)),
                new Lexem(LexemType.IntegerNumber,  new Location(8, 1, 9), "1"),
                new Lexem(LexemType.EOF,            new Location(9, 1, 10))
        );

        assertEquals(expected, lexems);
    }



    @Test
    public void parseShouldReturnListOfLexemsIfInputIsOutWithDivideExpr() {
        List<Lexem> lexems = lexer.parse("out 5 / 2");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Out,            new Location(0, 1, 1)),
                new Lexem(LexemType.IntegerNumber,  new Location(4, 1, 5), "5"),
                new Lexem(LexemType.Divide,         new Location(6, 1, 7)),
                new Lexem(LexemType.IntegerNumber,  new Location(8, 1, 9), "2"),
                new Lexem(LexemType.EOF,            new Location(9, 1, 10))
        );

        assertEquals(expected, lexems);
    }

    @Test
    public void parseShouldReturnListOfLexemsIfInputIsOutWithPowerExpr() {
        List<Lexem> lexems = lexer.parse("out 5 ^ 2");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Out,            new Location(0, 1, 1)),
                new Lexem(LexemType.IntegerNumber,  new Location(4, 1, 5), "5"),
                new Lexem(LexemType.Power,          new Location(6, 1, 7)),
                new Lexem(LexemType.IntegerNumber,  new Location(8, 1, 9), "2"),
                new Lexem(LexemType.EOF,            new Location(9, 1, 10))
        );

        assertEquals(expected, lexems);
    }

    @Test
    public void testSimpleAssignToVarA() {
        List<Lexem> lexems = lexer.parse("var a = 1");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store,          new Location(0, 1,  1), "a"),
                new Lexem(LexemType.IntegerNumber,  new Location(8, 1,  9), "1"),
                new Lexem(LexemType.EOF,            new Location(9, 1, 10))
        );

        assertEquals(expected, lexems);
    }


    @Test
    public void testParseIdentifierWhichStartsFromMap() throws Exception {
        String source = "var mapped = map({0, 1}, e -> e)\n";


        List<Lexem> lexems = lexer.parse(source);

        Lexem assign = lexems.get(0);
        assertEquals("First should be Store{mapped}", LexemType.Store, assign.getType());
        assertEquals("First should be Store{mapped}", "mapped", assign.getContent());
    }
}