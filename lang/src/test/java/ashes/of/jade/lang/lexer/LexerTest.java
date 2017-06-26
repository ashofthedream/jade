package ashes.of.jade.lang.lexer;

import ashes.of.jade.lang.Location;
import ashes.of.jade.lang.parser.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;


public class LexerTest {
    private static final Logger log = LogManager.getLogger(LexerTest.class);


    @Test
    public void parseShouldReturnOnlyEofIfInputIsEmpty() {
        String source = "";

        Lexer lexer = new Lexer();
        List<Lexem> actual = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.EOF, new Location(0, 1, 1))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void testMultilineAssign() {
        String source = "var first = 1\n" +
                        "var second = 2\n" +
                        "var third = first + second";

        Lexer lexer = new Lexer();
        List<Lexem> actual = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store, new Location(0, 1, 1), "first"),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 13), "1"),
                new Lexem(LexemType.NewLine, new Location(0, 1, 14)),

                new Lexem(LexemType.Store, new Location(0, 2, 1), "second"),
                new Lexem(LexemType.IntegerNumber, new Location(0, 2, 14), "2"),
                new Lexem(LexemType.NewLine, new Location(0, 2, 15)),

                new Lexem(LexemType.Store, new Location(0, 3, 1), "third"),
                new Lexem(LexemType.Load, new Location(0, 3, 13), "first"),
                new Lexem(LexemType.Plus, new Location(0, 3, 19)),
                new Lexem(LexemType.Load, new Location(0, 3, 21), "second"),
                new Lexem(LexemType.EOF, new Location(0, 3, 27))
        );

        assertEquals(expected, actual);
    }


    @Test
    public void parseShouldThrowAnExceptionIfLineStartsFromPlus() {
        String source = "+10\n";

        try {
            Lexer lexer = new Lexer();
            lexer.parse(source);

            fail("Parse should fail");
        } catch (ParseException e) {
            assertEquals(new Location(0, 1, 1), e.getLocation());
        }
    }

    @Test
    public void parseShouldThrowAnExceptionIfLineStartsFromMinus() {
        String source = "-10\n";

        try {
            Lexer lexer = new Lexer();
            lexer.parse(source);

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(0, 1, 1), e.getLocation());
        }
    }

    @Test
    public void parseShouldThrowAnExceptionIfLineStartsFromString() {
        String source = "\"hello world\"\n";

        try {
            Lexer lexer = new Lexer();
            lexer.parse(source);

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(0, 1, 1), e.getLocation());
        }
    }

    @Test
    public void parseShouldThrowAnExceptionIfLineStartsFromInteger() {
        String source = "13 + 37\n";

        try {
            Lexer lexer = new Lexer();
            lexer.parse(source);

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(0, 1, 1), e.getLocation());
        }
    }

    @Test
    public void parseShouldThrowAnExceptionIfLineStartsFromDouble() {
        String source = "13.37\n";

        try {
            Lexer lexer = new Lexer();
            lexer.parse(source);

            fail("Parse should fail");
        } catch (ParseException e) {
            assertEquals(new Location(0, 1, 1), e.getLocation());
        }
    }

    @Test
    public void parseShouldThrowAnExceptionIfLineStartsFromIdentifier() {
        String source = "a = 1\n";

        try {
            Lexer lexer = new Lexer();
            lexer.parse(source);

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(0, 1, 1), e.getLocation());
        }
    }

    @Test
    public void parseShouldThrowAnExceptionIfSecondLineStartsFromIdentifier() {
        String source = "var a = 5\n" +
                        "b = 1\n";

        try {
            Lexer lexer = new Lexer();
            lexer.parse(source);

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(0, 2, 1), e.getLocation());
        }
    }


    @Test
    public void parseShouldThrowAnExceptionIfSymbolIsUnknown() {
        String source = "var first = 1 & 7\n";

        try {
            Lexer lexer = new Lexer();
            lexer.parse(source);

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(0, 1, 15), e.getLocation());
        }
    }

    @Test
    public void parseShouldNormallyReadPositiveDoubles() {
        String source = "var positive = +1.0\n";

        Lexer lexer = new Lexer();
        List<Lexem> actual = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store, new Location(0, 1, 1), "positive"),
                new Lexem(LexemType.DoubleNumber, new Location(0, 1, 16), "+1.0"),
                new Lexem(LexemType.NewLine, new Location(0, 1, 20)),
                new Lexem(LexemType.EOF, new Location(0, 2, 1))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void parseShouldNormallyReadNegativeDoubles() {
        String source = "var negative = -1.0\n";

        Lexer lexer = new Lexer();
        List<Lexem> actual = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store, new Location(0, 1, 1), "negative"),
                new Lexem(LexemType.DoubleNumber, new Location(0, 1, 16), "-1.0"),
                new Lexem(LexemType.NewLine, new Location(0, 1, 20)),
                new Lexem(LexemType.EOF, new Location(0, 2, 1))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void parseShouldThrowAnExceptionIfDoubleNumberContainsMoreThanTwoDots() {
        String source = "var negative = -1..0\n";

        try {
            Lexer lexer = new Lexer();
            lexer.parse(source);

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(0, 1, 16), e.getLocation());
        }
    }

    @Test
    public void parseShouldNormallyReadPositiveIntegers() {
        String source = "var positive = +1";

        Lexer lexer = new Lexer();
        List<Lexem> actual = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store, new Location(0, 1, 1), "positive"),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 16), "+1"),
                new Lexem(LexemType.EOF, new Location(0, 1, 18))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void parseShouldNormallyReadNegativeIntegers() {
        String source = "var negative = -1";

        Lexer lexer = new Lexer();
        List<Lexem> actual = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store, new Location(0, 1, 1), "negative"),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 16), "-1"),
                new Lexem(LexemType.EOF, new Location(0, 1, 18))
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
                Lexer lexer = new Lexer();
                lexer.parse(source);

                fail("Parse should fail");
            } catch (ParseException e) {
                log.warn("Can't parse", e);
                assertEquals(new Location(0, 1, 19), e.getLocation());
            }
        }

    }

    @Test
    public void parseShouldNormallyReadEmptyStrings() {
        String source = "var empty = \"\"";

        Lexer lexer = new Lexer();
        List<Lexem> actual = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store, new Location(0, 1, 1), "empty"),
                new Lexem(LexemType.String, new Location(0, 1, 13), ""),
                new Lexem(LexemType.EOF, new Location(0, 1, 15))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void parseShouldNormallyReadNonEmptyStrings() {
        String source = "var str = \"this is a string\"";

        Lexer lexer = new Lexer();
        List<Lexem> actual = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store, new Location(0, 1, 1), "str"),
                new Lexem(LexemType.String, new Location(0, 1, 11), "this is a string"),
                new Lexem(LexemType.EOF, new Location(0, 1, 29))
        );

        assertEquals(expected, actual);
    }


    @Test
    public void parseShouldNormallyReadNonEmptyStringsWithEscapedDoubleQuotes() {
        String source = "var str = \"a \\\"string\\\" with escaped double quotes\"";

        Lexer lexer = new Lexer();
        List<Lexem> actual = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store, new Location(0, 1, 1), "str"),
                new Lexem(LexemType.String, new Location(0, 1, 11), "a \"string\" with escaped double quotes"),
                new Lexem(LexemType.EOF, new Location(0, 1, 52))
        );

        assertEquals(expected, actual);
    }


    @Test
    public void parseShouldThrowAnExceptionIfStringIsNotEndsWithDoubleQuote() {
        String source = "var str = \"a string...";

        try {
            Lexer lexer = new Lexer();
            lexer.parse(source);

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(0, 1, 11), e.getLocation());
        }
    }

    @Test
    public void parseShouldNormallyReadNegativeIntegersInExpr() {
        String source = "var negative = 5 - -1";

        Lexer lexer = new Lexer();
        List<Lexem> actual = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store, new Location(0, 1, 1), "negative"),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 16), "5"),
                new Lexem(LexemType.Minus, new Location(0, 1, 18)),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 20), "-1"),
                // but in real life EOF should be in 1:21...
                new Lexem(LexemType.EOF, new Location(0, 1, 22))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void parseShouldNormallyReadPositiveIntegersInExpr() {
        String source = "var positive = 5 - +1";

        Lexer lexer = new Lexer();
        List<Lexem> actual = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store, new Location(0, 1, 1), "positive"),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 16), "5"),
                new Lexem(LexemType.Minus, new Location(0, 1, 18)),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 20), "+1"),
                // but in real life EOF should be in 1:21...
                new Lexem(LexemType.EOF, new Location(0, 1, 22))
        );

        assertEquals(expected, actual);
    }

    @Test
    public void parseShouldThrowAnExceptionIfMultiplyOperatorIsSecondInExpr() {
        String source = "var a = 5 - * 1";

        try {
            Lexer lexer = new Lexer();
            lexer.parse(source);

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(0, 1, 13), e.getLocation());
        }
    }

    @Test
    public void parseShouldThrowAnExceptionIfDivideOperatorIsSecondInExpr() {
        String source = "var a = 5 - / 1";

        try {
            Lexer lexer = new Lexer();
            lexer.parse(source);

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(0, 1, 13), e.getLocation());
        }
    }



    @Test
    public void parseShouldThrowAnExceptionIfPowerOperatorIsSecondInExpr() {
        String source = "var a = 5 - ^ 1";

        try {
            Lexer lexer = new Lexer();
            lexer.parse(source);

            fail("Parse should fail");
        } catch (ParseException e) {
            log.warn("Can't parse", e);
            assertEquals(new Location(0, 1, 13), e.getLocation());
        }
    }

    @Test
    public void parseShouldReturnListOfLexemsIsInputIsOnlyMap() {
        String source = "var a = map({0, 1}, e -> e)";
        Lexer lexer = new Lexer();


        List<Lexem> lexems = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store, new Location(0, 1, 1), "a"),
                new Lexem(LexemType.Map, new Location(0, 1, 9)),
                new Lexem(LexemType.ParentOpen, new Location(0, 1, 12)),
                new Lexem(LexemType.CurlyOpen, new Location(0, 1, 13)),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 14), "0"),
                new Lexem(LexemType.Comma, new Location(0, 1, 15)),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 17), "1"),
                new Lexem(LexemType.CurlyClose, new Location(0, 1, 18)),
                new Lexem(LexemType.Comma, new Location(0, 1, 19)),

                new Lexem(LexemType.Load, new Location(0, 1, 21), "e"),
                new Lexem(LexemType.Arrow, new Location(0, 1, 23)),
                new Lexem(LexemType.Load, new Location(0, 1, 26), "e"),
                new Lexem(LexemType.ParentClose, new Location(0, 1, 27)),
                new Lexem(LexemType.EOF, new Location(0, 1, 28))
        );

        assertEquals(expected, lexems);
    }



    @Test
    public void parseShouldReturnListOfLexemsIsInputIsOnlyOutWithString() {
        String source = "print \"hello wold\"";
        Lexer lexer = new Lexer();


        List<Lexem> lexems = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Print, new Location(0, 1, 1)),
                new Lexem(LexemType.String, new Location(0, 1, 7), "hello wold"),
                new Lexem(LexemType.EOF, new Location(0, 1, 19))
        );

        assertEquals(expected, lexems);
    }

    @Test
    public void parseShouldReturnListOfLexemsIsInputIsOnlyOutWithNumber() {
        String source = "out 1";
        Lexer lexer = new Lexer();


        List<Lexem> lexems = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Out, new Location(0, 1, 1)),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 5), "1"),
                new Lexem(LexemType.EOF, new Location(0, 1, 6))
        );

        assertEquals(expected, lexems);
    }

    @Test
    public void parseShouldReturnListOfLexemsIsInputIsOnlyReduce() {
        String source = "var a = reduce({0, 1}, -7, a b -> a + b)";
        Lexer lexer = new Lexer();


        List<Lexem> lexems = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store, new Location(0, 1, 1), "a"),
                new Lexem(LexemType.Reduce, new Location(0, 1, 9)),
                new Lexem(LexemType.ParentOpen, new Location(0, 1, 15)),
                new Lexem(LexemType.CurlyOpen, new Location(0, 1, 16)),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 17), "0"),
                new Lexem(LexemType.Comma, new Location(0, 1, 18)),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 20), "1"),
                new Lexem(LexemType.CurlyClose, new Location(0, 1, 21)),
                new Lexem(LexemType.Comma, new Location(0, 1, 22)),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 24), "-7"),
                new Lexem(LexemType.Comma, new Location(0, 1, 26)),

                new Lexem(LexemType.Load, new Location(0, 1, 28), "a"),
                new Lexem(LexemType.Load, new Location(0, 1, 30), "b"),
                new Lexem(LexemType.Arrow, new Location(0, 1, 32)),
                new Lexem(LexemType.Load, new Location(0, 1, 35), "a"),
                new Lexem(LexemType.Plus, new Location(0, 1, 37)),
                new Lexem(LexemType.Load, new Location(0, 1, 39), "b"),
                new Lexem(LexemType.ParentClose, new Location(0, 1, 40)),
                new Lexem(LexemType.EOF, new Location(0, 1, 41))
        );

        assertEquals(expected, lexems);
    }

    @Test
    public void parseShouldReturnValidOutputIfInputIsAssignSequenceToSeqVar() {
        String source = "var seq = {0, 1}";
        Lexer lexer = new Lexer();


        List<Lexem> lexems = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store, new Location(0, 1, 1), "seq"),
                new Lexem(LexemType.CurlyOpen, new Location(0, 1, 11)),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 12), "0"),
                new Lexem(LexemType.Comma, new Location(0, 1, 13)),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 15), "1"),
                new Lexem(LexemType.CurlyClose, new Location(0, 1, 16)),
                new Lexem(LexemType.EOF, new Location(0, 1, 17))
        );

        assertEquals(expected, lexems);
    }


    @Test
    public void parseShouldReturnValidOutputIfInputIsAssignSequenceWithNegativeNumbersToSeqVar() {
        String source = "var seq = {-1, 1}";
        Lexer lexer = new Lexer();


        List<Lexem> lexems = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store, new Location(0, 1, 1), "seq"),
                new Lexem(LexemType.CurlyOpen, new Location(0, 1, 11)),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 12), "-1"),
                new Lexem(LexemType.Comma, new Location(0, 1, 14)),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 16), "1"),
                new Lexem(LexemType.CurlyClose, new Location(0, 1, 17)),
                new Lexem(LexemType.EOF, new Location(0, 1, 18))
        );

        assertEquals(expected, lexems);
    }

    @Test
    public void parseShouldReturnListOfLexemsIfInputIsOutWithPlusExpr() {
        String source = "out -5 + 8";
        Lexer lexer = new Lexer();


        List<Lexem> lexems = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Out, new Location(0, 1, 1)),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 5), "-5"),
                new Lexem(LexemType.Plus, new Location(0, 1, 8)),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 10), "8"),
                new Lexem(LexemType.EOF, new Location(0, 1, 11))
        );

        assertEquals(expected, lexems);
    }


    @Test
    public void parseShouldReturnListOfLexemsIfInputIsOutWithMinusExpr() {
        String source = "out 5 - 3";
        Lexer lexer = new Lexer();


        List<Lexem> lexems = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Out, new Location(0, 1, 1)),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 5), "5"),
                new Lexem(LexemType.Minus, new Location(0, 1, 7)),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 9), "3"),
                new Lexem(LexemType.EOF, new Location(0, 1, 10))
        );

        assertEquals(expected, lexems);
    }

    @Test
    public void parseShouldReturnListOfLexemsIfInputIsOutWithMultiplyExpr() {
        String source = "out 5 * 1";
        Lexer lexer = new Lexer();


        List<Lexem> lexems = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Out, new Location(0, 1, 1)),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 5), "5"),
                new Lexem(LexemType.Multiply, new Location(0, 1, 7)),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 9), "1"),
                new Lexem(LexemType.EOF, new Location(0, 1, 10))
        );

        assertEquals(expected, lexems);
    }



    @Test
    public void parseShouldReturnListOfLexemsIfInputIsOutWithDivideExpr() {
        String source = "out 5 / 2";
        Lexer lexer = new Lexer();


        List<Lexem> lexems = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Out, new Location(0, 1, 1)),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 5), "5"),
                new Lexem(LexemType.Divide, new Location(0, 1, 7)),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 9), "2"),
                new Lexem(LexemType.EOF, new Location(0, 1, 10))
        );

        assertEquals(expected, lexems);
    }

    @Test
    public void parseShouldReturnListOfLexemsIfInputIsOutWithPowerExpr() {
        String source = "out 5 ^ 2";
        Lexer lexer = new Lexer();


        List<Lexem> lexems = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Out, new Location(0, 1, 1)),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 5), "5"),
                new Lexem(LexemType.Power, new Location(0, 1, 7)),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 9), "2"),
                new Lexem(LexemType.EOF, new Location(0, 1, 10))
        );

        assertEquals(expected, lexems);
    }

    @Test
    public void testSimpleAssignToVarA() {
        String source = "var a = 1";
        Lexer lexer = new Lexer();


        List<Lexem> lexems = lexer.parse(source);

        List<Lexem> expected = Arrays.asList(
                new Lexem(LexemType.Store, new Location(0, 1, 1), "a"),
                new Lexem(LexemType.IntegerNumber, new Location(0, 1, 9), "1"),
                new Lexem(LexemType.EOF, new Location(0, 1, 10))
        );

        assertEquals(expected, lexems);
    }


    @Test
    public void testParseIdentifierWhichStartsFromMap() throws Exception {
        String source = "var mapped = map({0, 1}, e -> e)\n";

        Lexer lexer = new Lexer();
        List<Lexem> lexems = lexer.parse(source);

        Lexem assign = lexems.get(0);
        assertEquals("First should be Store{mapped}", LexemType.Store, assign.getType());
        assertEquals("First should be Store{mapped}", "mapped", assign.getContent());
    }
}