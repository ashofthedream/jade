package ashes.of.jade.lang.lexer;

import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.nodes.Node;
import ashes.of.jade.lang.parser.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Deque;
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
                new Lexem(LexemType.VAR,            new Location(0, 1, 1), "var"),
                new Lexem(LexemType.IDENTIFIER,     new Location(4, 1, 5), "first"),
                new Lexem(LexemType.EQUAL,          new Location(10, 1, 11)),
                new Lexem(LexemType.INTEGER,        new Location(12, 1, 13), "1"),
                new Lexem(LexemType.NL,             new Location(13, 1, 14)),

                new Lexem(LexemType.VAR,            new Location(14, 2, 1), "var"),
                new Lexem(LexemType.IDENTIFIER,     new Location(18, 2, 5), "second"),
                new Lexem(LexemType.EQUAL,          new Location(25, 2, 12)),
                new Lexem(LexemType.INTEGER,        new Location(27, 2, 14), "2"),
                new Lexem(LexemType.NL,             new Location(28, 2, 15)),

                new Lexem(LexemType.VAR,            new Location(29, 3, 1), "var"),
                new Lexem(LexemType.IDENTIFIER,     new Location(33, 3, 5), "third"),
                new Lexem(LexemType.EQUAL,          new Location(39, 3, 11)),
                new Lexem(LexemType.IDENTIFIER,     new Location(41, 3, 13), "first"),
                new Lexem(LexemType.PLUS,           new Location(47, 3, 19)),
                new Lexem(LexemType.IDENTIFIER,     new Location(49, 3, 21), "second"),
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
                new Lexem(LexemType.VAR,            new Location(0, 1, 1), "var"),
                new Lexem(LexemType.IDENTIFIER,     new Location(4, 1, 5), "positive"),
                new Lexem(LexemType.EQUAL,          new Location(13, 1, 14)),
                new Lexem(LexemType.DOUBLE,         new Location(15, 1, 16), "+1.0"),
                new Lexem(LexemType.NL,             new Location(19, 1, 20)),
                new Lexem(LexemType.EOF,            new Location(20, 2, 1))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void parseShouldNormallyReadNegativeDoubles() {
        List<Lexem> actual = lexer.parse("var negative = -1.0\n");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.VAR,            new Location(0, 1, 1), "var"),
                new Lexem(LexemType.IDENTIFIER,     new Location(4, 1, 5), "negative"),
                new Lexem(LexemType.EQUAL,          new Location(13, 1, 14)),
                new Lexem(LexemType.DOUBLE,         new Location(15, 1, 16), "-1.0"),
                new Lexem(LexemType.NL,             new Location(19, 1, 20)),
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
                new Lexem(LexemType.VAR,            new Location(0, 1, 1), "var"),
                new Lexem(LexemType.IDENTIFIER,     new Location(4, 1, 5), "positive"),
                new Lexem(LexemType.EQUAL,          new Location(13, 1, 14)),
                new Lexem(LexemType.INTEGER,        new Location(15, 1, 16), "+1"),
                new Lexem(LexemType.EOF,            new Location(17, 1, 18))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void parseShouldNormallyReadNegativeIntegers() {
        List<Lexem> actual = lexer.parse("var negative = -1");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.VAR,            new Location(0, 1, 1), "var"),
                new Lexem(LexemType.IDENTIFIER,     new Location(4, 1, 5), "negative"),
                new Lexem(LexemType.EQUAL,          new Location(13, 1, 14)),
                new Lexem(LexemType.INTEGER,        new Location(15, 1, 16), "-1"),
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
                new Lexem(LexemType.VAR,            new Location(0, 1, 1), "var"),
                new Lexem(LexemType.IDENTIFIER,     new Location(4, 1, 5), "empty"),
                new Lexem(LexemType.EQUAL,          new Location(10, 1, 11)),
                new Lexem(LexemType.STRING,         new Location(12, 1, 13), ""),
                new Lexem(LexemType.EOF,            new Location(14, 1, 15))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void parseShouldNormallyReadNonEmptyStrings() {
        List<Lexem> actual = lexer.parse("var str = \"this is a string\"");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.VAR,            new Location(0, 1, 1), "var"),
                new Lexem(LexemType.IDENTIFIER,     new Location(4, 1, 5), "str"),
                new Lexem(LexemType.EQUAL,          new Location(8, 1, 9)),
                new Lexem(LexemType.STRING,         new Location(10, 1, 11), "this is a string"),
                new Lexem(LexemType.EOF,            new Location(28, 1, 29))
        );

        assertEquals(expected, actual);
    }


    @Test
    public void parseShouldNormallyReadNonEmptyStringsWithEscapedDoubleQuotes() {
        List<Lexem> actual = lexer.parse("var str = \"a \\\"string\\\" with escaped double quotes\"");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.VAR,            new Location(0, 1, 1), "var"),
                new Lexem(LexemType.IDENTIFIER,     new Location(4, 1, 5), "str"),
                new Lexem(LexemType.EQUAL,          new Location(8, 1, 9)),
                new Lexem(LexemType.STRING,         new Location(10, 1, 11), "a \"string\" with escaped double quotes"),
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
                new Lexem(LexemType.VAR,            new Location(0, 1, 1), "var"),
                new Lexem(LexemType.IDENTIFIER,     new Location(4, 1, 5), "negative"),
                new Lexem(LexemType.EQUAL,          new Location(13, 1, 14)),
                new Lexem(LexemType.INTEGER,        new Location(15, 1, 16), "5"),
                new Lexem(LexemType.MINUS,          new Location(17, 1, 18)),
                new Lexem(LexemType.INTEGER,        new Location(19, 1, 20), "-1"),
                // but in real life EOF should be in 1:21...
                new Lexem(LexemType.EOF,            new Location(21, 1, 22))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void parseShouldNormallyReadPositiveIntegersInExpr() {
        List<Lexem> actual = lexer.parse("var positive = 5 - +1");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.VAR,            new Location(0, 1, 1), "var"),
                new Lexem(LexemType.IDENTIFIER,     new Location(4, 1, 5), "positive"),
                new Lexem(LexemType.EQUAL,          new Location(13, 1, 14)),
                new Lexem(LexemType.INTEGER,        new Location(15, 1, 16), "5"),
                new Lexem(LexemType.MINUS,          new Location(17, 1, 18)),
                new Lexem(LexemType.INTEGER,        new Location(19, 1, 20), "+1"),
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
                new Lexem(LexemType.VAR,            new Location(0, 1, 1), "var"),
                new Lexem(LexemType.IDENTIFIER,     new Location(4, 1, 5), "a"),
                new Lexem(LexemType.EQUAL,          new Location(6, 1, 7)),
                new Lexem(LexemType.MAP,            new Location(8, 1, 9), "map"),
                new Lexem(LexemType.PARENT_OPEN,    new Location(11, 1, 12)),
                new Lexem(LexemType.CURLY_OPEN,     new Location(12, 1, 13)),
                new Lexem(LexemType.INTEGER,        new Location(13, 1, 14), "0"),
                new Lexem(LexemType.COMMA,          new Location(14, 1, 15)),
                new Lexem(LexemType.INTEGER,        new Location(16, 1, 17), "1"),
                new Lexem(LexemType.CURLY_CLOSE,    new Location(17, 1, 18)),
                new Lexem(LexemType.COMMA,          new Location(18, 1, 19)),

                new Lexem(LexemType.IDENTIFIER,     new Location(20, 1, 21), "e"),
                new Lexem(LexemType.ARROW,          new Location(22, 1, 23)),
                new Lexem(LexemType.IDENTIFIER,     new Location(25, 1, 26), "e"),
                new Lexem(LexemType.PARENT_CLOSE,   new Location(26, 1, 27)),
                new Lexem(LexemType.EOF,            new Location(27, 1, 28))
        );

        assertEquals(expected, lexems);
    }



    @Test
    public void parseShouldReturnListOfLexemsIsInputIsOnlyOutWithString() {
        List<Lexem> lexems = lexer.parse("print \"hello wold\"");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.PRINT,          new Location(0, 1, 1), "print"),
                new Lexem(LexemType.STRING,         new Location(6, 1, 7), "hello wold"),
                new Lexem(LexemType.EOF,            new Location(18, 1, 19))
        );

        assertEquals(expected, lexems);
    }

    @Test
    public void parseShouldReturnListOfLexemsIsInputIsOnlyOutWithNumber() {
        List<Lexem> lexems = lexer.parse("out 1");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.OUT,            new Location(0, 1, 1), "out"),
                new Lexem(LexemType.INTEGER,        new Location(4, 1, 5), "1"),
                new Lexem(LexemType.EOF,            new Location(5, 1, 6))
        );

        assertEquals(expected, lexems);
    }

    @Test
    public void parseShouldReturnListOfLexemsIsInputIsOnlyReduce() {
        List<Lexem> lexems = lexer.parse("var a = reduce({0, 1}, -7, a b -> a + b)");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.VAR,            new Location(0, 1, 1), "var"),
                new Lexem(LexemType.IDENTIFIER,     new Location(4, 1, 5), "a"),
                new Lexem(LexemType.EQUAL,          new Location(6, 1, 7)),
                new Lexem(LexemType.REDUCE,         new Location(8, 1, 9), "reduce"),
                new Lexem(LexemType.PARENT_OPEN,    new Location(14, 1, 15)),
                new Lexem(LexemType.CURLY_OPEN,     new Location(15, 1, 16)),
                new Lexem(LexemType.INTEGER,        new Location(16, 1, 17), "0"),
                new Lexem(LexemType.COMMA,          new Location(17, 1, 18)),
                new Lexem(LexemType.INTEGER,        new Location(19, 1, 20), "1"),
                new Lexem(LexemType.CURLY_CLOSE,    new Location(20, 1, 21)),
                new Lexem(LexemType.COMMA,          new Location(21, 1, 22)),
                new Lexem(LexemType.INTEGER,        new Location(23, 1, 24), "-7"),
                new Lexem(LexemType.COMMA,          new Location(25, 1, 26)),

                new Lexem(LexemType.IDENTIFIER,     new Location(27, 1, 28), "a"),
                new Lexem(LexemType.IDENTIFIER,     new Location(29, 1, 30), "b"),
                new Lexem(LexemType.ARROW,          new Location(31, 1, 32)),
                new Lexem(LexemType.IDENTIFIER,     new Location(34, 1, 35), "a"),
                new Lexem(LexemType.PLUS,           new Location(36, 1, 37)),
                new Lexem(LexemType.IDENTIFIER,     new Location(38, 1, 39), "b"),
                new Lexem(LexemType.PARENT_CLOSE,   new Location(39, 1, 40)),
                new Lexem(LexemType.EOF,            new Location(40, 1, 41))
        );

        assertEquals(expected, lexems);
    }

    @Test
    public void parseShouldReturnValidOutputIfInputIsAssignSequenceToSeqVar() {
        List<Lexem> lexems = lexer.parse("var seq = {0, 1}");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.VAR,            new Location(0, 1, 1), "var"),
                new Lexem(LexemType.IDENTIFIER,     new Location(4, 1, 5), "seq"),
                new Lexem(LexemType.EQUAL,          new Location(8, 1, 9)),
                new Lexem(LexemType.CURLY_OPEN,     new Location(10, 1, 11)),
                new Lexem(LexemType.INTEGER,        new Location(11, 1, 12), "0"),
                new Lexem(LexemType.COMMA,          new Location(12, 1, 13)),
                new Lexem(LexemType.INTEGER,        new Location(14, 1, 15), "1"),
                new Lexem(LexemType.CURLY_CLOSE,    new Location(15, 1, 16)),
                new Lexem(LexemType.EOF,            new Location(16, 1, 17))
        );

        assertEquals(expected, lexems);
    }


    @Test
    public void parseShouldReturnValidOutputIfInputIsAssignSequenceWithNegativeNumbersToSeqVar() {
        List<Lexem> lexems = lexer.parse("var seq = {-1, 1}");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.VAR,            new Location(0, 1, 1), "var"),
                new Lexem(LexemType.IDENTIFIER,     new Location(4, 1, 5), "seq"),
                new Lexem(LexemType.EQUAL,          new Location(8, 1, 9)),
                new Lexem(LexemType.CURLY_OPEN,     new Location(10, 1, 11)),
                new Lexem(LexemType.INTEGER,        new Location(11, 1, 12), "-1"),
                new Lexem(LexemType.COMMA,          new Location(13, 1, 14)),
                new Lexem(LexemType.INTEGER,        new Location(15, 1, 16), "1"),
                new Lexem(LexemType.CURLY_CLOSE,    new Location(16, 1, 17)),
                new Lexem(LexemType.EOF,            new Location(17, 1, 18))
        );

        assertEquals(expected, lexems);
    }

    @Test
    public void parseShouldReturnListOfLexemsIfInputIsOutWithPlusExpr() {
        List<Lexem> lexems = lexer.parse("out -5 + 8");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.OUT,            new Location(0, 1, 1), "out"),
                new Lexem(LexemType.INTEGER,        new Location(4, 1, 5), "-5"),
                new Lexem(LexemType.PLUS,           new Location(7, 1, 8)),
                new Lexem(LexemType.INTEGER,        new Location(9, 1, 10), "8"),
                new Lexem(LexemType.EOF,            new Location(10, 1, 11))
        );

        assertEquals(expected, lexems);
    }


    @Test
    public void parseShouldReturnListOfLexemsIfInputIsOutWithMinusExpr() {
        List<Lexem> lexems = lexer.parse("out 5 - 3");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.OUT,            new Location(0, 1, 1), "out"),
                new Lexem(LexemType.INTEGER,        new Location(4, 1, 5), "5"),
                new Lexem(LexemType.MINUS,          new Location(6, 1, 7)),
                new Lexem(LexemType.INTEGER,        new Location(8, 1, 9), "3"),
                new Lexem(LexemType.EOF,            new Location(9, 1, 10))
        );

        assertEquals(expected, lexems);
    }

    @Test
    public void parseShouldReturnListOfLexemsIfInputIsOutWithMultiplyExpr() {
        List<Lexem> lexems = lexer.parse("out 5 * 1");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.OUT,            new Location(0, 1, 1), "out"),
                new Lexem(LexemType.INTEGER,        new Location(4, 1, 5), "5"),
                new Lexem(LexemType.MULTIPLY,       new Location(6, 1, 7)),
                new Lexem(LexemType.INTEGER,        new Location(8, 1, 9), "1"),
                new Lexem(LexemType.EOF,            new Location(9, 1, 10))
        );

        assertEquals(expected, lexems);
    }



    @Test
    public void parseShouldReturnListOfLexemsIfInputIsOutWithDivideExpr() {
        List<Lexem> lexems = lexer.parse("out 5 / 2");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.OUT,            new Location(0, 1, 1), "out"),
                new Lexem(LexemType.INTEGER,        new Location(4, 1, 5), "5"),
                new Lexem(LexemType.DIVIDE,         new Location(6, 1, 7)),
                new Lexem(LexemType.INTEGER,        new Location(8, 1, 9), "2"),
                new Lexem(LexemType.EOF,            new Location(9, 1, 10))
        );

        assertEquals(expected, lexems);
    }

    @Test
    public void parseShouldReturnListOfLexemsIfInputIsOutWithPowerExpr() {
        List<Lexem> lexems = lexer.parse("out 5 ^ 2");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.OUT,            new Location(0, 1, 1), "out"),
                new Lexem(LexemType.INTEGER,        new Location(4, 1, 5), "5"),
                new Lexem(LexemType.POWER,          new Location(6, 1, 7)),
                new Lexem(LexemType.INTEGER,        new Location(8, 1, 9), "2"),
                new Lexem(LexemType.EOF,            new Location(9, 1, 10))
        );

        assertEquals(expected, lexems);
    }

    @Test
    public void testSimpleAssignToVarA() {
        List<Lexem> lexems = lexer.parse("var a = 1");

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.VAR,            new Location(0, 1,  1), "var"),
                new Lexem(LexemType.IDENTIFIER,     new Location(4, 1,  5), "a"),
                new Lexem(LexemType.EQUAL,          new Location(6, 1,  7)),
                new Lexem(LexemType.INTEGER,        new Location(8, 1,  9), "1"),
                new Lexem(LexemType.EOF,            new Location(9, 1, 10))
        );

        assertEquals(expected, lexems);
    }


    @Test
    public void parseShouldFail1IfStmtStartsFromMap() {
        try {
            List<Lexem> lexems = lexer.parse("map({0, 1}, x -> x)");

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(0, 1, 1), e.getLocation());
        }
    }


    @Test
    public void parseShouldFail1IfStmtStartsFromReduce() {
        try {
            List<Lexem> lexems = lexer.parse("reduce({0, 1}, x -> x)");

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(0, 1, 1), e.getLocation());
        }
    }


    @Test
    public void parseShouldFail1IfPrintIsInMiddleOfStmt() {
        try {
            List<Lexem> lexems = lexer.parse("out print");

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(4, 1, 5), e.getLocation());
        }
    }

    @Test
    public void parseShouldFail1IfOutIsInMiddleOfStmt() {
        try {
            List<Lexem> lexems = lexer.parse("print 1 + out");

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(10, 1, 11), e.getLocation());
        }
    }

    @Test
    public void parseShouldFail1IfVarIsInMiddleOfStmt() {
        try {
            List<Lexem> lexems = lexer.parse("var a = var b + 7");

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(8, 1, 9), e.getLocation());
        }
    }

    @Test
    public void testParseIdentifierWhichStartsFromMap() throws Exception {
        List<Lexem> lexems = lexer.parse("var mapped = map({0, 1}, e -> e)\n");

        Lexem actual = lexems.get(1);
        assertEquals("Second should be Store{mapped}",
                new Lexem(LexemType.IDENTIFIER, new Location(4, 1, 5), "mapped"), actual);
    }

    @Test
    public void parseShouldThrowAnExceptionIfExpressionContainsTwoDoublesWithoutOperation() {
        try {
            List<Lexem> lexems = lexer.parse("var a = 2.0 0.4");

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(12, 1, 13), e.getLocation());
        }
    }
}